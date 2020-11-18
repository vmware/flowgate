/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vcworker.scheduler.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.cis.tagging.CategoryModel;
import com.vmware.cis.tagging.CategoryModel.Cardinality;
import com.vmware.cis.tagging.TagModel;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.redis.message.AsyncService;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.EventUser;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.common.utils.IPAddressUtil;
import com.vmware.flowgate.vcworker.client.HostTagClient;
import com.vmware.flowgate.vcworker.client.VsphereClient;
import com.vmware.flowgate.vcworker.config.ServiceKeyConfig;
import com.vmware.flowgate.vcworker.model.EsxiMetadata;
import com.vmware.flowgate.vcworker.model.HostInfo;
import com.vmware.flowgate.vcworker.model.HostNic;
import com.vmware.flowgate.vcworker.model.VCConstants;
import com.vmware.vim.binding.vim.AboutInfo;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datastore.HostMount;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.cluster.ConfigInfoEx;
import com.vmware.vim.binding.vim.cluster.DpmHostConfigInfo;
import com.vmware.vim.binding.vim.fault.InvalidLogin;
import com.vmware.vim.binding.vim.host.Capability;
import com.vmware.vim.binding.vim.host.NetworkInfo;
import com.vmware.vim.binding.vim.host.PhysicalNic;
import com.vmware.vim.binding.vim.host.PhysicalNic.LinkSpeedDuplex;
import com.vmware.vim.binding.vim.host.RuntimeInfo;
import com.vmware.vim.binding.vim.host.Summary;
import com.vmware.vim.binding.vim.host.Summary.HardwareSummary;
import com.vmware.vim.binding.vim.host.Summary.QuickStats;
import com.vmware.vim.binding.vim.host.ConnectInfo.DatastoreInfo;
import com.vmware.vim.binding.vim.host.MountInfo;
import com.vmware.vim.binding.vmodl.DynamicProperty;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.query.InvalidProperty;
import com.vmware.vim.binding.vmodl.query.PropertyCollector;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.FilterSpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.ObjectContent;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.ObjectSpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.PropertySpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.RetrieveOptions;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.RetrieveResult;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.SelectionSpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.TraversalSpec;
import com.vmware.vim.vmomi.client.exception.ConnectionException;
import com.vmware.vim.binding.impl.vmodl.TypeNameImpl;


@Service
public class VCDataService implements AsyncService {

   private static final Logger logger = LoggerFactory.getLogger(VCDataService.class);
   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private MessagePublisher publisher;

   @Autowired
   private StringRedisTemplate template;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;

   private ObjectMapper mapper = new ObjectMapper();

   @Override
   @Async("asyncServiceExecutor")
   public void executeAsync(EventMessage message) {
      // when receive message, will do the related jobs
      // sync customer attribute.
      // update the value.
      if (message.getType() != EventType.VCenter) {
         logger.warn("Drop none vcenter message " + message.getType());
         return;
      }
      // TO, this should be comment out since it may contain vc password.
      logger.info("message received");
      Set<EventUser> users = message.getTarget().getUsers();

      for (EventUser command : users) {
         logger.info(command.getId());
         switch (command.getId()) {
         case EventMessageUtil.VCENTER_SyncData:
            // it will sync all the data depend on the type in the vcjoblist.
            String messageString = null;
            while ((messageString =
                  template.opsForList().rightPop(EventMessageUtil.vcJobList)) != null) {
               EventMessage payloadMessage = null;
               try {
                  payloadMessage = mapper.readValue(messageString, EventMessageImpl.class);
               } catch (IOException e) {
                  logger.error("Cannot process message", e);
               }
               if (payloadMessage == null) {
                  continue;
               }
               SDDCSoftwareConfig vcInfo = null;
               try {
                  vcInfo = mapper.readValue(payloadMessage.getContent(), SDDCSoftwareConfig.class);
               } catch (IOException e) {
                  logger.error("Cannot process message", e);
               }
               if (null == vcInfo) {
                  continue;
               }

               for (EventUser payloadCommand : payloadMessage.getTarget().getUsers()) {
                  switch (payloadCommand.getId()) {
                  case EventMessageUtil.VCENTER_SyncCustomerAttrs:
                     syncCustomAttributes(vcInfo);
                     logger.info("Finish sync customer attributes for " + vcInfo.getName());
                     break;
                  case EventMessageUtil.VCENTER_SyncCustomerAttrsData:
                     syncCustomerAttrsData(vcInfo);
                     logger.info("Finish sync data for " + vcInfo.getName());
                     break;
                  case EventMessageUtil.VCENTER_QueryHostMetaData:
                     queryHostMetaData(vcInfo);
                     logger.info("Finish query host metadata for " + vcInfo.getName());
                     break;
                  default:
                     break;
                  }
               }
            }
            break;
         case EventMessageUtil.VCENTER_SyncCustomerAttrs:
            logger.warn(
                  "VCENTER_SyncCustomerAttrs command is depreacted. use VCENTER_SyncData instead");
            break;
         case EventMessageUtil.VCENTER_SyncCustomerAttrsData:
            logger.warn(
                  "VCENTER_SyncCustomerAttrsData command is depreacted. use VCENTER_SyncData instead");
            break;
         default:
            logger.warn("Not supported command");
            break;
         }
      }
   }

   public void updateIntegrationStatus(SDDCSoftwareConfig config) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      restClient.updateSDDC(config);
   }

   public void checkAndUpdateIntegrationStatus(SDDCSoftwareConfig vc, String message) {
      IntegrationStatus integrationStatus = vc.getIntegrationStatus();
      if (integrationStatus == null) {
         integrationStatus = new IntegrationStatus();
      }
      int timesOftry = integrationStatus.getRetryCounter();
      timesOftry++;
      if (timesOftry < FlowgateConstant.MAXNUMBEROFRETRIES) {
         integrationStatus.setRetryCounter(timesOftry);
      } else {
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(message);
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         logger.error("Failed to sync data to VC");
      }
      vc.setIntegrationStatus(integrationStatus);
      updateIntegrationStatus(vc);
   }

   public VsphereClient connectVsphere(SDDCSoftwareConfig vc) throws Exception {
      return VsphereClient.connect(String.format(VCConstants.SDKURL, vc.getServerURL()),
            vc.getUserName(), vc.getPassword(), !vc.isVerifyCert());
   }

   public HashMap<String, ServerMapping> getVaildServerMapping(SDDCSoftwareConfig vc) {

      HashMap<String, ServerMapping> mobIdDictionary = new HashMap<String, ServerMapping>();
      ServerMapping[] mappings = null;
      try {
         restClient.setServiceKey(serviceKeyConfig.getServiceKey());
         mappings = restClient.getServerMappingsByVC(vc.getId()).getBody();
      } catch (HttpClientErrorException clientError) {
         if (clientError.getRawStatusCode() != HttpStatus.NOT_FOUND.value()) {
            return null;
         }
      }

      for (ServerMapping mapping : mappings) {
         if (mapping.getAsset() != null) {
            mobIdDictionary.put(mapping.getVcMobID(), mapping);
         }
      }
      return mobIdDictionary;

   }

   public void queryHostMetaData(SDDCSoftwareConfig vc) {

      HashMap<String, ServerMapping> serverMappingMap = getVaildServerMapping(vc);
      if (serverMappingMap == null) {
         logger.info("serverMapping is invaild");
         return;
      }

      try (VsphereClient vsphereClient = connectVsphere(vc);) {
         
         String vcInstanceUUID = vsphereClient.getVCUUID();
         Collection<HostSystem> hosts = vsphereClient.getAllHost();
         if (hosts == null || hosts.isEmpty()) {
            logger.error("vsphere: " + vsphereClient.getVCUUID() + " get hosts is null");
            return;
         }
         Collection<ClusterComputeResource> clusters = vsphereClient.getAllClusterComputeResource();
         HashMap<String, ClusterComputeResource> clusterMap =
               new HashMap<String, ClusterComputeResource>();
         for (ClusterComputeResource cluster : clusters) {
            clusterMap.put(cluster._getRef().getValue(), cluster);
         }

         for (HostSystem host : hosts) {

            String mobId = host._getRef().getValue();
            if (serverMappingMap.containsKey(mobId)) {
               ServerMapping serverMapping = serverMappingMap.get(mobId);
               String assetId = serverMapping.getAsset();

               Asset hostMappingAsset = restClient.getAssetByID(assetId).getBody();
               if (hostMappingAsset == null) {
                  logger.error("serverMapping: " + serverMapping.getId() + " get asset: " + assetId
                        + " is null");
                  continue;
               }
               HostInfo hostInfo = new HostInfo();
               HashMap<String, String> hostJustification =
                     hostMappingAsset.getJustificationfields();
               if (hostJustification != null && !hostJustification.isEmpty()) {
                  String oldHostInfoString = hostJustification.get(FlowgateConstant.HOST_METADATA);
                  if (oldHostInfoString != null) {
                     try {
                        hostInfo = mapper.readValue(oldHostInfoString, HostInfo.class);
                     } catch (IOException e) {
                        logger.error("Cannot process message", e);
                        continue;
                     }
                  }
               }

               boolean hostNeedUpdate = false;
               boolean clusterNeedUpdate = false;
               hostNeedUpdate = feedHostMetaData(host, hostInfo);
               if (clusters != null && !clusters.isEmpty()) {
                  clusterNeedUpdate =
                        feedClusterMetaData(clusterMap, host, hostInfo, vcInstanceUUID);
               }

               if (hostNeedUpdate || clusterNeedUpdate) {

                  try {

                     String vcHostObjStr = mapper.writeValueAsString(hostInfo);
                     hostJustification.put(FlowgateConstant.HOST_METADATA, vcHostObjStr);
                     hostMappingAsset.setJustificationfields(hostJustification);
                  } catch (JsonProcessingException e) {
                     logger.error("Format host info map error", e);
                     continue;
                  }
                  restClient.saveAssets(hostMappingAsset);
               } else {
                  logger.debug("host: " + mobId + " No update required");
                  continue;
               }
            }
         }
      } catch (ConnectionException e1) {
         checkAndUpdateIntegrationStatus(vc, e1.getMessage());
         return;
      } catch (ExecutionException e2) {
         if (e2.getCause() instanceof InvalidLogin) {
            logger.error("Failed to push data to " + vc.getServerURL(), e2);
            checkAndUpdateIntegrationStatus(vc, "Invalid username or password.");
            return;
         }
      } catch (Exception e) {
         logger.error("Failed to sync the host metadata to VC ", e);
         return;
      }
   }

   public boolean feedClusterMetaData(HashMap<String, ClusterComputeResource> clusterMap,
         HostSystem host, HostInfo hostInfo, String vcInstanceUUID) {

      ManagedObjectReference hostParent = host.getParent();
      if (hostParent == null || !hostParent.getType().equals(VCConstants.CLUSTERCOMPUTERESOURCE)) {
         return false;
      }
      String clusterMobId = hostParent.getValue();
      ClusterComputeResource cluster = clusterMap.get(clusterMobId);
      boolean needUpdate = false;
      EsxiMetadata oldEsxiMetadata = hostInfo.getEsxiMetadata();
      if (oldEsxiMetadata == null) {
         needUpdate = true;
         oldEsxiMetadata = new EsxiMetadata();
      }
      String hostMobId = host._getRef().getValue();

      ConfigInfoEx configInfoExtension = (ConfigInfoEx) cluster.getConfigurationEx();

      String oldEsxiMetadataClusterName = oldEsxiMetadata.getClusterName();
      String esxiMetadataClusterName = cluster.getName();
      if (!StringUtils.equals(oldEsxiMetadataClusterName, esxiMetadataClusterName)) {
         needUpdate = true;
         oldEsxiMetadata.setClusterName(esxiMetadataClusterName);
      }

      if (!configInfoExtension.getDpmConfigInfo().getEnabled()
            .equals(oldEsxiMetadata.isClusterDPMenabled())) {
         needUpdate = true;
         oldEsxiMetadata.setClusterDPMenabled(configInfoExtension.getDpmConfigInfo().getEnabled());
      }

      String oldEsxiMetadataClusterDRSBehavior = oldEsxiMetadata.getClusterDRSBehavior();
      String esxiMetadataClusterDRSBehavior = configInfoExtension.getDrsConfig().getDefaultVmBehavior().toString();
      if (!StringUtils.equals(oldEsxiMetadataClusterDRSBehavior, esxiMetadataClusterDRSBehavior)) {
         needUpdate = true;
         oldEsxiMetadata.setClusterDRSBehavior(esxiMetadataClusterDRSBehavior);
      }

      if (cluster.getSummary().getNumEffectiveHosts() != oldEsxiMetadata
            .getClusterEffectiveHostsNum()) {
         needUpdate = true;
         oldEsxiMetadata.setClusterEffectiveHostsNum(cluster.getSummary().getNumEffectiveHosts());
      }

      if (cluster.getSummary().getNumHosts() != oldEsxiMetadata.getClusterHostsNum()) {
         needUpdate = true;
         oldEsxiMetadata.setClusterHostsNum(cluster.getSummary().getNumHosts());
      }

      if (cluster.getSummary().getTotalCpu() != oldEsxiMetadata.getClusterTotalCpu()) {
         needUpdate = true;
         oldEsxiMetadata.setClusterTotalCpu(cluster.getSummary().getTotalCpu());
      }

      if (cluster.getSummary().getNumCpuCores() != oldEsxiMetadata.getClusterTotalCpuCores()) {
         needUpdate = true;
         oldEsxiMetadata.setClusterTotalCpuCores(cluster.getSummary().getNumCpuCores());
      }

      if (cluster.getSummary().getNumCpuThreads() != oldEsxiMetadata.getClusterTotalCpuThreads()) {
         needUpdate = true;
         oldEsxiMetadata.setClusterTotalCpuThreads(cluster.getSummary().getNumCpuThreads());
      }

      if (cluster.getSummary().getTotalMemory() != oldEsxiMetadata.getClusterTotalMemory()) {
         needUpdate = true;
         oldEsxiMetadata.setClusterTotalMemory(cluster.getSummary().getTotalMemory());
      }

      if (!configInfoExtension.getVsanConfigInfo().getEnabled()
            .equals(oldEsxiMetadata.isHostVSANenabled())) {
         needUpdate = true;
         oldEsxiMetadata
               .setClusterVSANenabled(configInfoExtension.getVsanConfigInfo().getEnabled());
      }

      DpmHostConfigInfo[] dpmHostConfigInfos = configInfoExtension.getDpmHostConfig();
      if (dpmHostConfigInfos != null && dpmHostConfigInfos.length > 0) {
         for (DpmHostConfigInfo dpmHostConfigInfo : dpmHostConfigInfos) {
            if (hostMobId.equals(dpmHostConfigInfo.getKey().getValue())) {
               if (!dpmHostConfigInfo.getEnabled().equals(oldEsxiMetadata.isHostDPMenabled())) {
                  needUpdate = true;
                  oldEsxiMetadata.setHostDPMenabled(dpmHostConfigInfo.getEnabled());
               }
            }
         }
      }

      String oldEsxiMetadataClusterMobid = oldEsxiMetadata.getClusterMobid();
      String esxiMetadataClusterMobid = cluster._getRef().getValue();
      if (!StringUtils.equals(oldEsxiMetadataClusterMobid, esxiMetadataClusterMobid)) {
         needUpdate = true;
         oldEsxiMetadata.setClusterMobid(esxiMetadataClusterMobid);
      }

      String oldEsxiMetadataInstanceId = oldEsxiMetadata.getInstanceId();
      if (!StringUtils.equals(oldEsxiMetadataInstanceId, vcInstanceUUID)) {
         needUpdate = true;
         oldEsxiMetadata.setInstanceId(vcInstanceUUID);
      }

      String oldEsxiMetadataHostName = oldEsxiMetadata.getHostName();
      String esxiMetadataHostName = host.getName();
      if (!StringUtils.equals(oldEsxiMetadataHostName, esxiMetadataHostName)) {
         needUpdate = true;
         oldEsxiMetadata.setHostName(host.getName());
      }

      if (!host.getConfig().getVsanHostConfig().getEnabled()
            .equals(oldEsxiMetadata.isHostVSANenabled())) {
         needUpdate = true;
         oldEsxiMetadata.setHostVSANenabled(host.getConfig().getVsanHostConfig().getEnabled());
      }

      if (!host.getCapability().getVsanSupported().equals(oldEsxiMetadata.isHostVsanSupported())) {
         needUpdate = true;
         oldEsxiMetadata.setHostVsanSupported(host.getCapability().getVsanSupported());
      }

      oldEsxiMetadata.setHostMobid(hostMobId);
      hostInfo.setEsxiMetadata(oldEsxiMetadata);

      return needUpdate;
   }

   public boolean feedHostMetaData(HostSystem host, HostInfo hostInfo) {

      boolean needUpdate = false;
      Capability capability = host.getCapability();
      if (capability != null) {

         if (capability.isMaintenanceModeSupported() != hostInfo.isMaintenanceModeSupported()) {
            hostInfo.setMaintenanceModeSupported(capability.isMaintenanceModeSupported());
            needUpdate = true;
         }

         if (capability.isRebootSupported() != hostInfo.isRebootSupported()) {
            hostInfo.setRebootSupported(capability.isRebootSupported());
            needUpdate = true;
         }

         Integer maxRunningVMs =
               capability.getMaxRunningVMs() == null ? 0 : capability.getMaxRunningVMs();
         if (maxRunningVMs.intValue() != hostInfo.getMaxRunningVms()) {
            hostInfo.setMaxRunningVms(maxRunningVMs);
            needUpdate = true;
         }

         Integer maxSupportedVcpus =
               capability.getMaxSupportedVcpus() == null ? 0 : capability.getMaxSupportedVcpus();
         if (maxSupportedVcpus.intValue() != hostInfo.getMaxSupportedVcpus()) {
            hostInfo.setMaxSupportedVcpus(maxSupportedVcpus);
            needUpdate = true;
         }

         Integer maxRegisteredVMs =
               capability.getMaxRegisteredVMs() == null ? 0 : capability.getMaxRegisteredVMs();
         if (maxRegisteredVMs.intValue() != hostInfo.getMaxRegisteredVMs()) {
            hostInfo.setMaxRegisteredVMs(maxRegisteredVMs);
            needUpdate = true;
         }
      }

      RuntimeInfo runtimeInfo = host.getRuntime();
      if (runtimeInfo != null) {

         String hostInfoConnectionState = hostInfo.getConnectionState();
         String runtimeInfoConnectionState = runtimeInfo.getConnectionState().toString();
         if (!StringUtils.equals(hostInfoConnectionState, runtimeInfoConnectionState)) {
            hostInfo.setConnectionState(runtimeInfoConnectionState);
            needUpdate = true;
         }

         String hostInfoPowerState = hostInfo.getPowerState();
         String runtimeInfoPowerState = runtimeInfo.getPowerState().toString();
         if (!StringUtils.equals(hostInfoPowerState, runtimeInfoPowerState)) {
            hostInfo.setPowerState(runtimeInfoPowerState);
            needUpdate = true;
         }

         if (runtimeInfo.getBootTime().getTimeInMillis() != hostInfo.getBootTime()) {
            hostInfo.setBootTime(runtimeInfo.getBootTime().getTimeInMillis());
            needUpdate = true;
         }
      }

      Summary summary = host.getSummary();
      if (summary != null) {

         if (summary.isRebootRequired() != hostInfo.isRebootRequired()) {
            hostInfo.setRebootRequired(summary.isRebootRequired());
            needUpdate = true;
         }

         QuickStats quickStats = summary.getQuickStats();
         if (quickStats != null) {
            Integer uptime = quickStats.getUptime() == null ? 0 : quickStats.getUptime();
            hostInfo.setUptime(uptime);
         }

         AboutInfo aboutInfo = summary.getConfig().getProduct();
         if (aboutInfo != null) {
            String build = aboutInfo.getBuild();
            String hostInfoHypervisorBuildVersion = hostInfo.getHypervisorBuildVersion();
            if (!StringUtils.equals(build, hostInfoHypervisorBuildVersion)) {
               hostInfo.setHypervisorBuildVersion(build);
               needUpdate = true;
            }

            String fullName = aboutInfo.getFullName();
            String hostInfoHypervisorFullName = hostInfo.getHypervisorFullName();
            if (!StringUtils.equals(fullName, hostInfoHypervisorFullName)) {
               hostInfo.setHypervisorFullName(fullName);
               needUpdate = true;
            }

            String licenseProductName = aboutInfo.getLicenseProductName();
            String hostInfoHypervisorLicenseProductName =
                  hostInfo.getHypervisorLicenseProductName();
            if (!StringUtils.equals(licenseProductName, hostInfoHypervisorLicenseProductName)) {
               hostInfo.setHypervisorLicenseProductName(licenseProductName);
               needUpdate = true;
            }

            String licenseProductVersion = aboutInfo.getLicenseProductVersion();
            String hostInfoHypervisorLicenseProductVersion =
                  hostInfo.getHypervisorLicenseProductVersion();
            if (!StringUtils.equals(licenseProductVersion, hostInfoHypervisorLicenseProductVersion)) {
               hostInfo.setHypervisorLicenseProductVersion(licenseProductVersion);
               needUpdate = true;
            }

            String version = aboutInfo.getVersion();
            String hostInfoHypervisorVersion = hostInfo.getHypervisorVersion();
            if (!StringUtils.equals(version, hostInfoHypervisorVersion)) {
               hostInfo.setHypervisorVersion(version);
               needUpdate = true;
            }

         }

         HardwareSummary hardwareSummary = summary.getHardware();
         if (hardwareSummary != null) {

            String hostModel = hardwareSummary.getModel();
            String hostInfoHostModel = hostInfo.getHostModel();
            if (!StringUtils.equals(hostModel, hostInfoHostModel)) {
               hostInfo.setHostModel(hostModel);
               needUpdate = true;
            }

            String vendor = hardwareSummary.getVendor();
            String hostInfoHypervisorVendor = hostInfo.getHypervisorVendor();
            if (!StringUtils.equals(vendor, hostInfoHypervisorVendor)) {
               hostInfo.setHypervisorVendor(vendor);
               needUpdate = true;
            }

            if (hardwareSummary.getNumCpuCores() != hostInfo.getCpuTotalCores()) {
               hostInfo.setCpuTotalCores(hardwareSummary.getNumCpuCores());
               needUpdate = true;
            }

            if (hardwareSummary.getNumCpuPkgs() != hostInfo.getCpuTotalPackages()) {
               hostInfo.setCpuTotalPackages(hardwareSummary.getNumCpuPkgs());
               needUpdate = true;
            }

            if (hardwareSummary.getNumCpuThreads() != hostInfo.getCpuTotalThreads()) {
               hostInfo.setCpuTotalThreads(hardwareSummary.getNumCpuThreads());
               needUpdate = true;
            }

            if (hardwareSummary.getCpuMhz() != hostInfo.getSingleCoreCpuMhz()) {
               hostInfo.setSingleCoreCpuMhz(hardwareSummary.getCpuMhz());
               needUpdate = true;
            }

            if (hardwareSummary.getMemorySize() != hostInfo.getMemoryCapacity()) {
               hostInfo.setMemoryCapacity(hardwareSummary.getMemorySize());
               needUpdate = true;
            }
         }
      }

      Map<String, HostNic> newHostNicMap = new HashMap<>();
      Map<String, HostNic> oldHostNicMap = new HashMap<>();

      NetworkInfo networkInfo = host.getConfig().getNetwork();
      if (networkInfo != null) {

         PhysicalNic[] physicalNics = networkInfo.getPnic();
         List<HostNic> hostNics = new ArrayList<HostNic>();

         for (PhysicalNic physicalNic : physicalNics) {

            HostNic hostNic = new HostNic();

            hostNic.setMacAddress(physicalNic.getMac());
            hostNic.setDriver(physicalNic.getDriver());
            LinkSpeedDuplex physicalNicLinkSpeed = physicalNic.getLinkSpeed();
            if (physicalNicLinkSpeed == null) {
               hostNic.setDuplex(false);
               hostNic.setLinkSpeedMb(0);
            } else {
               hostNic.setDuplex(physicalNicLinkSpeed.isDuplex());
               hostNic.setLinkSpeedMb(physicalNicLinkSpeed.getSpeedMb());
            }
            hostNic.setName(physicalNic.getDevice());
            hostNics.add(hostNic);
            newHostNicMap.put(physicalNic.getMac(), hostNic);
         }

         int nicsNum = physicalNics.length;
         if (nicsNum > 0) {
            List<HostNic> oldNics = hostInfo.getHostNics();
            if (oldNics == null) {
               hostInfo.setHostNics(hostNics);
               needUpdate = true;
            } else {
               for (HostNic oldhostNic : oldNics) {
                  oldHostNicMap.put(oldhostNic.getMacAddress(), oldhostNic);
               }
               if (newHostNicMap.size() != oldHostNicMap.size()) {
                  hostInfo.setHostNics(hostNics);
                  needUpdate = true;
               }else {
                  for (Entry<String, HostNic> entry : oldHostNicMap.entrySet()) {
                     if(!newHostNicMap.containsKey(entry.getKey())) {
                        hostInfo.setHostNics(hostNics);
                        needUpdate = true;
                        break;
                     }
                  }
               }
            }
         }
      }

      long diskCapacity = 0;
      DatastoreInfo[] datastores = host.queryConnectionInfo().getDatastore();
      if (datastores != null && datastores.length > 0) {
         for (DatastoreInfo datastore : datastores) {
            //TODO: only count writable data store.
            //count shared data store for each host, 
            //which will cause problem in calculate the total available storage among multiple hosts.
            diskCapacity += datastore.getSummary().getCapacity();
         }
         if (diskCapacity != hostInfo.getDiskCapacity()) {
            needUpdate = true;
            hostInfo.setDiskCapacity(diskCapacity);
         }
      }

      return needUpdate;
   }
   
   private void syncCustomAttributes(SDDCSoftwareConfig vc) {
      // TODO need to allow only update 1 vcenter instead of all the vcenter.

      try (VsphereClient vsphereClient =
            VsphereClient.connect(String.format(VCConstants.SDKURL, vc.getServerURL()),
                  vc.getUserName(), vc.getPassword(), !vc.isVerifyCert());) {
         for (String key : VCConstants.hostCustomAttrMapping.values()) {
            vsphereClient.createCustomAttribute(key, VCConstants.HOSTSYSTEM);
         }
         // Add the PDU information;
         vsphereClient.createCustomAttribute(VCConstants.ASSET_PDUs, VCConstants.HOSTSYSTEM);
         // Add host switch information;
         vsphereClient.createCustomAttribute(VCConstants.ASSET_SWITCHs, VCConstants.HOSTSYSTEM);
      } catch (ConnectionException e1) {
         checkAndUpdateIntegrationStatus(vc, e1.getMessage());
         return;
      } catch (ExecutionException e2) {
         if (e2.getCause() instanceof InvalidLogin) {
            logger.error("Failed to push data to " + vc.getServerURL(), e2);
            IntegrationStatus integrationStatus = vc.getIntegrationStatus();
            if (integrationStatus == null) {
               integrationStatus = new IntegrationStatus();
            }
            integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
            integrationStatus.setDetail("Invalid username or password.");
            integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
            updateIntegrationStatus(vc);
            return;
         }
      } catch (Exception e) {
         logger.error("Failed to sync the host metadata to VC ", e);
         return;
      }
      try (HostTagClient client = new HostTagClient(vc.getServerURL(), vc.getUserName(),
            vc.getPassword(), !vc.isVerifyCert());) {
         client.initConnection();
         TagModel tag = client.getTagByName(VCConstants.locationAntiAffinityTagName);
         String categoryID;
         if (tag == null) {
            CategoryModel category = client.getTagCategoryByName(VCConstants.categoryName);
            if (category == null) {
               categoryID = client.createTagCategory(VCConstants.categoryName,
                     VCConstants.categoryDescription, Cardinality.MULTIPLE);
            } else {
               categoryID = category.getId();
            }
            client.createTag(VCConstants.locationAntiAffinityTagName,
                  VCConstants.locationAntiAffinityTagDescription, categoryID);
         }
      } catch (Exception e) {
         logger.error("Faild to check the predefined tag information", e);
      }
   }

   private void syncCustomerAttrsData(SDDCSoftwareConfig vcInfo) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());

      try (VsphereClient vsphereClient =
            VsphereClient.connect(String.format(VCConstants.SDKURL, vcInfo.getServerURL()),
                  vcInfo.getUserName(), vcInfo.getPassword(), !vcInfo.isVerifyCert());) {
         ServerMapping[] mappings = null;
         try {
            mappings = restClient.getServerMappingsByVC(vcInfo.getId()).getBody();
         } catch (HttpClientErrorException clientError) {
            if (clientError.getRawStatusCode() != HttpStatus.NOT_FOUND.value()) {
               throw clientError;
            }
            mappings = new ServerMapping[0];
         }
         HashMap<String, ServerMapping> mobIdDictionary = new HashMap<String, ServerMapping>();
         for (ServerMapping mapping : mappings) {
            mobIdDictionary.put(mapping.getVcMobID(), mapping);
         }
         List<ServerMapping> validMapping = new ArrayList<ServerMapping>();
         Collection<HostSystem> hosts = vsphereClient.getAllHost();
         Map<String, HostSystem> hostDictionary = new HashMap<String, HostSystem>();
         for (HostSystem host : hosts) {
            String mobId = host._getRef().getValue();
            String hostName = host.getName();
            if (mobIdDictionary.containsKey(mobId)) {
               ServerMapping serverMapping = mobIdDictionary.get(mobId);
               if (!serverMapping.getVcHostName().equals(hostName)) {
                  // need to update the hostname.
                  serverMapping.setVcHostName(hostName);
                  restClient.saveServerMapping(serverMapping);
               }
               if (serverMapping.getAsset() != null) {
                  validMapping.add(serverMapping);
               } else {
                  // check the hostNameIP mapping
                  String ipaddress = IPAddressUtil.getIPAddress(hostName);
                  if (null != ipaddress) {
                     AssetIPMapping[] ipMappings =
                           restClient.getHostnameIPMappingByIP(ipaddress).getBody();
                     if (null != ipMappings && ipMappings.length > 0) {
                        // update the mapping
                        String assetName = ipMappings[0].getAssetname();
                        Asset asset = restClient.getAssetByName(assetName).getBody();
                        if (asset != null) {
                           serverMapping.setAsset(asset.getId());
                           restClient.saveServerMapping(serverMapping);
                           validMapping.add(serverMapping);
                        }
                     } else {// seems we don't have the ip hostname mapping. Notify infoblox to check the ip
                        logger.info("Notify infoblox to check ip: " + ipaddress);
                        publisher.publish(null, ipaddress);
                     }
                  }
               }
               hostDictionary.put(mobId, host);
            } else {
               ServerMapping newMapping = new ServerMapping();
               newMapping.setVcHostName(hostName);
               newMapping.setVcID(vcInfo.getId());
               newMapping.setVcMobID(mobId);
               newMapping.setVcInstanceUUID(vsphereClient.getVCUUID());
               String ipaddress = IPAddressUtil.getIPAddress(hostName);
               logger.info(String.format("hostName %s, ipaddress: %s", hostName, ipaddress));
               // publish message to queue.
               if (null != ipaddress) {
                  logger.info("Notify infoblox");
                  publisher.publish(null, hostName);
                  AssetIPMapping[] ipMappings =
                        restClient.getHostnameIPMappingByIP(ipaddress).getBody();
                  if (null != ipMappings && ipMappings.length > 0) {
                     // update the mapping
                     String assetName = ipMappings[0].getAssetname();
                     Asset asset = restClient.getAssetByName(assetName).getBody();
                     if (asset != null) {
                        newMapping.setAsset(asset.getId());
                     }
                  }
               }
               restClient.saveServerMapping(newMapping);
            }
         }
         // feed meta data to VC.
         Asset[] assets = restClient.getAssetsByVCID(vcInfo.getId()).getBody();
         Map<String, Asset> assetDictionary = new HashMap<String, Asset>();
         for (Asset asset : assets) {
            assetDictionary.put(asset.getId(), asset);
         }

         feedData(assetDictionary, validMapping, hostDictionary);
         validClusterHostsLocationAntiaffinity(vcInfo, assetDictionary, validMapping);
      } catch (ConnectionException e1) {
         checkAndUpdateIntegrationStatus(vcInfo, e1.getMessage());
         return;
      } catch (ExecutionException e2) {
         if (e2.getCause() instanceof InvalidLogin) {
            logger.error("Failed to push data to " + vcInfo.getServerURL(), e2);
            IntegrationStatus integrationStatus = vcInfo.getIntegrationStatus();
            if (integrationStatus == null) {
               integrationStatus = new IntegrationStatus();
            }
            integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
            integrationStatus.setDetail("Invalid username or password.");
            integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
            updateIntegrationStatus(vcInfo);
            return;
         }
      } catch (Exception e) {
         logger.error("Failed to push data to " + vcInfo.getServerURL(), e);
      }
   }

   private void feedData(Map<String, Asset> assetDictionary, List<ServerMapping> validMapping,
         Map<String, HostSystem> hostDictionary) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      for (ServerMapping validServer : validMapping) {
         HostSystem host = hostDictionary.get(validServer.getVcMobID());
         Asset asset = assetDictionary.get(validServer.getAsset());
         BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(asset);
         for (String key : VCConstants.hostCustomAttrMapping.keySet()) {
            host.setCustomValue(VCConstants.hostCustomAttrMapping.get(key),
                  String.valueOf(wrapper.getPropertyValue(key)));
         }
         Map<String, String> idNamePortMapping = getPDUSwitchIDNamePortMapping(asset);
         if (asset.getPdus() != null) {
            // check pdu and port
            List<String> pduNameList = new ArrayList<String>();
            for (String pduid : asset.getPdus()) {
               if (idNamePortMapping.containsKey(pduid)) {
                  pduNameList.add(idNamePortMapping.get(pduid));
               } else {
                  Asset pduAsset = restClient.getAssetByID(pduid).getBody();
                  if (pduAsset != null) {
                     pduNameList.add(pduAsset.getAssetName());
                  }
               }
            }
            host.setCustomValue(VCConstants.ASSET_PDUs, String.join(",", pduNameList));
         }
         if (asset.getSwitches() != null) {
            List<String> switchNameList = new ArrayList<String>();
            for (String switchID : asset.getSwitches()) {
               if (idNamePortMapping.containsKey(switchID)) {
                  switchNameList.add(idNamePortMapping.get(switchID));
               } else {
                  Asset switchAsset = restClient.getAssetByID(switchID).getBody();
                  if (switchAsset != null) {
                     switchNameList.add(switchAsset.getAssetName());
                  }
               }
            }
            host.setCustomValue(VCConstants.ASSET_SWITCHs, String.join(",", switchNameList));
         }
      }
   }

   public Map<String, String> getPDUSwitchIDNamePortMapping(Asset asset) {
      Map<String, String> result = new HashMap<String, String>();
      Map<String, String> enhanceFields = asset.getJustificationfields();
      if (null != enhanceFields) {
         String allPduPortString = enhanceFields.get(FlowgateConstant.PDU_PORT_FOR_SERVER);
         List<String> devicePorts = new ArrayList<String>();
         if (!StringUtils.isEmpty(allPduPortString)) {
            devicePorts = Arrays.asList(allPduPortString.split(FlowgateConstant.SPILIT_FLAG));
         }

         String allSwitchPortString = enhanceFields.get(FlowgateConstant.NETWORK_PORT_FOR_SERVER);
         if (!StringUtils.isEmpty(allSwitchPortString)) {
            devicePorts
                  .addAll(Arrays.asList(allSwitchPortString.split(FlowgateConstant.SPILIT_FLAG)));
         }

         for (String devicePortString : devicePorts) {
            // startport_FIELDSPLIT_endDeviceName_FIELDSPLIT_endport_FIELDSPLIT_endDeviceAssetID
            // item[0] start port
            // item[1] device name
            // item[2] end port
            // itme[3] assetid
            String items[] = devicePortString.split(FlowgateConstant.SEPARATOR);
            result.put(items[3], items[1] + ":" + items[2]);
         }
      }
      return result;
   }

   private void validClusterHostsLocationAntiaffinity(SDDCSoftwareConfig vcInfo,
         Map<String, Asset> assetDictionary, List<ServerMapping> validMapping) {

      Map<String, Set<Asset>> assetsByCluster = new HashMap<String, Set<Asset>>();
      for (ServerMapping validServer : validMapping) {
         Asset asset = assetDictionary.get(validServer.getAsset());
         if (null != validServer.getVcClusterMobID()) {
            if (!assetsByCluster.containsKey(validServer.getVcClusterMobID())) {
               assetsByCluster.put(validServer.getVcClusterMobID(), new HashSet<Asset>());
            }
            assetsByCluster.get(validServer.getVcClusterMobID()).add(asset);
         }
      }
      Set<Asset> needTagHost = new HashSet<>();
      for (String clusterMob : assetsByCluster.keySet()) {
         if (assetsByCluster.get(clusterMob).size() > 1) {
            String location = "%s-%s-%s-%s-%s-%s-%d";
            Map<String, Set<Asset>> assetsByLocation = new HashMap<String, Set<Asset>>();
            for (Asset asset : assetsByCluster.get(clusterMob)) {
               String assetLocation = String.format(location, asset.getRegion(), asset.getCountry(),
                     asset.getCity(), asset.getBuilding(), asset.getFloor(),
                     asset.getCabinetAssetNumber(), asset.getCabinetUnitPosition());
               if (!assetsByLocation.containsKey(assetLocation)) {
                  assetsByLocation.put(assetLocation, new HashSet<Asset>());
               }
               assetsByLocation.get(assetLocation).add(asset);
            }
            for (String local : assetsByLocation.keySet()) {
               if (assetsByLocation.get(local).size() > 1) {
                  // now we need to tag the hosts
                  needTagHost.addAll(assetsByLocation.get(local));
               }
            }
         }
      }

      if (!needTagHost.isEmpty()) {
         Map<String, ServerMapping> assetIDMapping = new HashMap<String, ServerMapping>();
         for (ServerMapping mapping : validMapping) {
            assetIDMapping.put(mapping.getAsset(), mapping);
         }
         try (HostTagClient client = new HostTagClient(vcInfo.getServerURL(), vcInfo.getUserName(),
               vcInfo.getPassword(), !vcInfo.isVerifyCert());) {
            client.initConnection();
            TagModel locationTag = client.getTagByName(VCConstants.locationAntiAffinityTagName);
            for (Asset a : needTagHost) {
               client.attachTagToHost(locationTag.getId(),
                     assetIDMapping.get(a.getId()).getVcMobID());
            }
         } catch (Exception e) {
            logger.warn("Failed to tag the host, will try to tag it in next run.", e);
         }
      }
   }

}
