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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.cis.tagging.CategoryModel;
import com.vmware.cis.tagging.CategoryModel.Cardinality;
import com.vmware.cis.tagging.TagModel;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.ValueUnit;
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
import com.vmware.vim.binding.impl.vim.PerformanceManager_Impl.QuerySpecImpl;
import com.vmware.vim.binding.vim.AboutInfo;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.PerformanceManager;
import com.vmware.vim.binding.vim.PerformanceManager.CounterInfo;
import com.vmware.vim.binding.vim.PerformanceManager.EntityMetric;
import com.vmware.vim.binding.vim.PerformanceManager.EntityMetricBase;
import com.vmware.vim.binding.vim.PerformanceManager.IntSeries;
import com.vmware.vim.binding.vim.PerformanceManager.MetricId;
import com.vmware.vim.binding.vim.PerformanceManager.MetricSeries;
import com.vmware.vim.binding.vim.PerformanceManager.ProviderSummary;
import com.vmware.vim.binding.vim.PerformanceManager.QuerySpec;
import com.vmware.vim.binding.vim.PerformanceManager.SampleInfo;
import com.vmware.vim.binding.vim.cluster.ConfigInfoEx;
import com.vmware.vim.binding.vim.cluster.DpmHostConfigInfo;
import com.vmware.vim.binding.vim.fault.InvalidLogin;
import com.vmware.vim.binding.vim.host.Capability;
import com.vmware.vim.binding.vim.host.ConnectInfo.DatastoreInfo;
import com.vmware.vim.binding.vim.host.NetworkInfo;
import com.vmware.vim.binding.vim.host.PhysicalNic;
import com.vmware.vim.binding.vim.host.PhysicalNic.LinkSpeedDuplex;
import com.vmware.vim.binding.vim.host.RuntimeInfo;
import com.vmware.vim.binding.vim.host.Summary;
import com.vmware.vim.binding.vim.host.Summary.HardwareSummary;
import com.vmware.vim.binding.vim.host.Summary.QuickStats;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.client.exception.ConnectionException;


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

   private static final double JOULETOKWHRATE = 2.7777777777777776E-7;

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
               } catch (IOException ioException) {
                  logger.error("Cannot process message", ioException);
               }
               if (payloadMessage == null) {
                  continue;
               }
               SDDCSoftwareConfig vcInfo = null;
               try {
                  vcInfo = mapper.readValue(payloadMessage.getContent(), SDDCSoftwareConfig.class);
               } catch (IOException ioException) {
                  logger.error("Cannot process message", ioException);
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
                  case EventMessageUtil.VCENTER_QueryHostUsageData:
                     queryHostMetrics(vcInfo);
                     logger.info("Finish query host usage for " + vcInfo.getName());
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

   public Map<String, ServerMapping> getVaildServerMapping(SDDCSoftwareConfig vc) {

      Map<String, ServerMapping> mobIdDictionary = new HashMap<String, ServerMapping>();
      ServerMapping[] mappings = null;
      try {
         restClient.setServiceKey(serviceKeyConfig.getServiceKey());
         mappings = restClient.getServerMappingsByVC(vc.getId()).getBody();
      } catch (HttpClientErrorException clientError) {
         if (clientError.getRawStatusCode() != HttpStatus.NOT_FOUND.value()) {
            return null;
         }
      }
      if(mappings == null) {
         logger.error("vc: {} get serverMappings is null.", vc.getId());
         return null;
      }
      for (ServerMapping mapping : mappings) {
         if (mapping.getAsset() != null) {
            mobIdDictionary.put(mapping.getVcMobID(), mapping);
         }
      }
      return mobIdDictionary;

   }


   public void queryHostMetaData(SDDCSoftwareConfig vc) {

      Map<String, ServerMapping> serverMappingMap = getVaildServerMapping(vc);
      if (serverMappingMap == null || serverMappingMap.isEmpty()) {
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
         Map<String, ClusterComputeResource> clusterMap =
               new HashMap<String, ClusterComputeResource>();
         if(clusters == null) {
            logger.error("vsphere: " + vsphereClient.getVCUUID() + " get clusters is null");
            return;
         }
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
                     } catch (IOException ioException) {
                        logger.error("Cannot process message", ioException);
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
                  } catch (JsonProcessingException jsonProcessingException) {
                     logger.error("Format host info map error", jsonProcessingException);
                     continue;
                  }
                  restClient.saveAssets(hostMappingAsset);
               } else {
                  logger.debug("host: " + mobId + " No update required");
                  continue;
               }
            }
         }
      } catch (ConnectionException connectionException) {
         checkAndUpdateIntegrationStatus(vc, connectionException.getMessage());
         return;
      } catch (ExecutionException executionException) {
         if (executionException.getCause() instanceof InvalidLogin) {
            logger.error("Failed to push data to " + vc.getServerURL(), executionException);
            checkAndUpdateIntegrationStatus(vc, "Invalid username or password.");
            return;
         }
      } catch (Exception exception) {
         logger.error("Failed to sync the host metadata to VC ", exception);
         return;
      }
   }

   public void queryHostMetrics(SDDCSoftwareConfig vc) {

      Map<String, ServerMapping> serverMappingMap = getVaildServerMapping(vc);
      if (serverMappingMap == null || serverMappingMap.isEmpty()) {
         logger.info("No serverMapping found for vc {}", vc.getName());
         return;
      }
      try (VsphereClient vsphereClient = connectVsphere(vc);) {

         Collection<HostSystem> hosts = vsphereClient.getAllHost();
         if (hosts == null || hosts.isEmpty()) {
            logger.error("vsphere: {} get hosts is null", vc.getName());
            return;
         }
         for (HostSystem host : hosts) {

            String mobId = host._getRef().getValue();
            if (serverMappingMap.containsKey(mobId)) {
               ServerMapping serverMapping = serverMappingMap.get(mobId);
               String assetId = serverMapping.getAsset();

               Asset hostMappingAsset = restClient.getAssetByID(assetId).getBody();
               if (hostMappingAsset == null) {
                  logger.error("The asset {} doesn't exist in serverMapping {}.", assetId, serverMapping.getId());
                  continue;
               }
               feedHostUsageData(vsphereClient, assetId, host._getRef());
            }
         }

      } catch (ConnectionException connectionException) {
         checkAndUpdateIntegrationStatus(vc, connectionException.getMessage());
         return;
      } catch (ExecutionException executionException) {
         if (executionException.getCause() instanceof InvalidLogin) {
            logger.error("Failed to login {}: {}" ,vc.getServerURL(), executionException);
            checkAndUpdateIntegrationStatus(vc, "Invalid username or password.");
            return;
         }
      } catch (Exception exception) {
         logger.error("Failed to sync the host metrics to VC ", exception);
         return;
      }
   }

   private void feedAssetMetricsFormulars(Asset asset) {
      String assetId = asset.getId();
      List<String> vcHostMetricsFormulaKeyList = new ArrayList<>(16);
      // cpu
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_CPUUSAGE);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_CPUUSEDINMHZ);
      // memory
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_ACTIVEMEMORY);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_BALLOONMEMORY);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_CONSUMEDMEMORY);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_SHAREDMEMORY);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_SWAPMEMORY);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_MEMORYUSAGE);
      // storage
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_STORAGEIORATEUSAGE);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_STORAGEUSED);
      // power
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_POWER);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_MINIMUM_USED_POWER);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_PEAK_USED_POWER);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_AVERAGE_USED_POWER);
      vcHostMetricsFormulaKeyList.add(MetricName.SERVER_ENERGY_CONSUMPTION);

      Map<String, String> metricsFormulas = asset.getMetricsformulars();
      Map<String, String> hostMetricsFormula = new HashMap<>();
      if (StringUtils.isNotBlank(metricsFormulas.get(FlowgateConstant.HOST_METRICS))) {
         hostMetricsFormula = asset.metricsFormulaToMap(metricsFormulas.get(FlowgateConstant.HOST_METRICS), new TypeReference<Map<String, String>>() {});
      }
      boolean isUpdated = false;
      for (String vcHostMetricsFormulaKey : vcHostMetricsFormulaKeyList) {
         if (!hostMetricsFormula.containsKey(vcHostMetricsFormulaKey)) {
            hostMetricsFormula.put(vcHostMetricsFormulaKey, assetId);
            isUpdated = true;
         }
      }
      if (isUpdated) {
         metricsFormulas.put(FlowgateConstant.HOST_METRICS, asset.metricsFormulaToString(hostMetricsFormula));
         asset.setMetricsformulars(metricsFormulas);
         restClient.saveAssets(asset);
      }
   }

   private Map<Integer, String> getMetricsCounters(PerformanceManager performanceManager) {

      CounterInfo[] counterInfos = performanceManager.getPerfCounter();

      Map<Integer, String> counters = new HashMap<Integer, String>();
      for (int i = 0; i < counterInfos.length; i++) {
         CounterInfo counterInfo = counterInfos[i];
         int counterId = counterInfo.getKey();
         String groupKey = counterInfo.getGroupInfo().getKey();
         String nameKey = counterInfo.getNameInfo().getKey();

         switch(groupKey) {
            case VCConstants.HOST_CPU_GROUP:
               switch(nameKey) {
                  case VCConstants.HOST_METRIC_USAGE:
                  case VCConstants.HOST_METRIC_USAGEMHZ:
                     counters.put(counterId, groupKey.concat(nameKey));
                     break;
               }
               break;
            case VCConstants.HOST_MEMORY_GROUP:
               switch(nameKey) {
                  case VCConstants.HOST_METRIC_USAGE:
                  case VCConstants.HOST_METRIC_MEM_ACTIVE:
                  case VCConstants.HOST_METRIC_MEM_SHARED:
                  case VCConstants.HOST_METRIC_MEM_CONSUMED:
                  case VCConstants.HOST_METRIC_MEM_SWAP:
                  case VCConstants.HOST_METRIC_MEM_BALLON:
                     counters.put(counterId, groupKey.concat(nameKey));
                     break;
               }
               break;
            case VCConstants.HOST_DISK_GROUP:
               switch(nameKey) {
                  case VCConstants.HOST_METRIC_USAGE:
                     counters.put(counterId, groupKey.concat(nameKey));
                     break;
               }
               break;
            case VCConstants.HOST_NETWORK_GROUP:
               switch(nameKey) {
                  case VCConstants.HOST_METRIC_USAGE:
                     counters.put(counterId, groupKey.concat(nameKey));
                     break;
               }
               break;
            case VCConstants.HOST_POWER_GROUP:
               switch(nameKey) {
                  case VCConstants.HOST_METRIC_POWER_POWER:
                  case VCConstants.HOST_METRIC_POWER_ENERGY:
                     counters.put(counterId, groupKey.concat(nameKey));
                     break;
               }
            default:
               break;
         }
      }

      return counters;
   }

   private List<MetricId> getPerformenceMetricsIds(PerformanceManager performanceManager, ManagedObjectReference hostRef, Map<Integer, String> counters) {

      MetricId[] queryAvailableMetric =
            performanceManager.queryAvailableMetric(hostRef, null, null, new Integer(20));

      List<MetricId> metricIdList = new ArrayList<MetricId>();
      if (queryAvailableMetric != null && queryAvailableMetric.length > 0) {
         for (MetricId metricId : queryAvailableMetric) {
            int counterId = metricId.getCounterId();
            String instanceId = metricId.getInstance();
            if (counters.containsKey(new Integer(counterId)) && (instanceId == null || instanceId.isEmpty())) {
               metricIdList.add(metricId);
            }
         }
      }

      return metricIdList;
   }

   public void feedHostUsageData(VsphereClient vsphereClient, String assetId,
         ManagedObjectReference hostRef) {

      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setAssetID(assetId);
      List<ValueUnit> valueUnits = new ArrayList<ValueUnit>();

      PerformanceManager performanceManager = vsphereClient.getPerformanceManager();

      Map<Integer, String> counters = getMetricsCounters(performanceManager);
      if(counters == null || counters.isEmpty()) {
         logger.error("Asset: {} failed to get performance counters", assetId);
         return;
      }
      List<MetricId> metricIdList = getPerformenceMetricsIds(performanceManager, hostRef, counters);
      if(metricIdList == null || metricIdList.isEmpty()) {
         logger.error("Asset: {} failed to get performance metricIds", assetId);
         return;
      }
      //fill spec info
      ProviderSummary summary = performanceManager.queryProviderSummary(hostRef);
      int perfInterval = summary.getRefreshRate();
      QuerySpec[] specs = new QuerySpecImpl[1];
      specs[0] = new QuerySpecImpl();
      specs[0].setEntity(hostRef);
      specs[0].setMetricId(metricIdList.toArray(new MetricId[metricIdList.size()]));
      specs[0].setIntervalId(perfInterval);
      specs[0].setMaxSample(15);

      //get metrics value
      EntityMetricBase[] metricBase = performanceManager.queryStats(specs);
      if(metricBase == null || metricBase.length <= 0) {
         logger.error("Asset: {} failed to get performance metricBase", assetId);
         return;
      }
      List<ValueUnit> powerValueUnits = new ArrayList<>(15);
      for(EntityMetricBase entityMetricBase : metricBase) {
         /*
         (vim.EntityMetric) {
               dynamicType = null,
               dynamicProperty = null,
               entity = ManagedObjectReference: type = HostSystem, value = host-65, serverGuid = null,
               sampleInfo = (vim.SampleInfo) [
            (vim.SampleInfo) {
               dynamicType = null,
               dynamicProperty = null,
               timestamp = java.util.GregorianCalendar[time=?,areFieldsSet=false,areAllFieldsSet=true,lenient=true,zone=sun.util.calendar.ZoneInfo[id="GMT+00:00",offset=0,dstSavings=0,useDaylight=false,transitions=0,lastRule=null],firstDayOfWeek=1,minimalDaysInFirstWeek=1,ERA=1,YEAR=2020,MONTH=11,WEEK_OF_YEAR=1,WEEK_OF_MONTH=1,DAY_OF_MONTH=10,DAY_OF_YEAR=1,DAY_OF_WEEK=5,DAY_OF_WEEK_IN_MONTH=1,AM_PM=0,HOUR=0,HOUR_OF_DAY=2,MINUTE=12,SECOND=20,MILLISECOND=0,ZONE_OFFSET=0,DST_OFFSET=0],
               interval = 20
            }...
         ],
         value = (vim.MetricSeries) [
            (vim.IntSeries) {
               dynamicType = null,
               dynamicProperty = null,
               id = (vim.MetricId) {
                  dynamicType = null,
                  dynamicProperty = null,
                  counterId = 125,
                  instance =
               },
               value = (LONG) [
                  667,
                  251,
                  96,
                  298,
                  99,
                  119,
                  492,
                  318,
                  98,
                  218,
                  128,
                  141,
                  1224,
                  358,
                  133
               ]
            }...
         */
         EntityMetric entityMetric = (EntityMetric) entityMetricBase;
         /*
         (vim.IntSeries) {
               dynamicType = null,
               dynamicProperty = null,
               id = (vim.MetricId) {
                  dynamicType = null,
                  dynamicProperty = null,
                  counterId = 125,
                  instance =
               },
               value = (LONG) [101,134,667,282,90,224,99,129,667,251,96,298,99,119,492]
            }
         */
         MetricSeries[] metricSeries = entityMetric.getValue();
         /*
         (vim.SampleInfo) {
            dynamicType = null,
            dynamicProperty = null,
            timestamp = java.util.GregorianCalendar[time=?,areFieldsSet=false,areAllFieldsSet=true,lenient=true,zone=sun.util.calendar.ZoneInfo[id="GMT+00:00",offset=0,dstSavings=0,useDaylight=false,transitions=0,lastRule=null],firstDayOfWeek=1,minimalDaysInFirstWeek=1,ERA=1,YEAR=2020,MONTH=11,WEEK_OF_YEAR=1,WEEK_OF_MONTH=1,DAY_OF_MONTH=10,DAY_OF_YEAR=1,DAY_OF_WEEK=5,DAY_OF_WEEK_IN_MONTH=1,AM_PM=0,HOUR=0,HOUR_OF_DAY=2,MINUTE=7,SECOND=40,MILLISECOND=0,ZONE_OFFSET=0,DST_OFFSET=0],
            interval = 20
          }
         */
         SampleInfo[] sampleInfos = entityMetric.getSampleInfo();
         for (MetricSeries metricSerie : metricSeries) {
            if (metricSerie instanceof IntSeries) {
               IntSeries intSeries = (IntSeries) metricSerie;
               long[] values = intSeries.getValue();
               int counterId = metricSerie.getId().getCounterId();
               for(int index = 0; index < values.length; index++) {
                  long timeStamp = sampleInfos[index].getTimestamp().getTimeInMillis();

                  ValueUnit valueUnit = new ValueUnit();
                  long value = values[index];

                  switch(counters.get(counterId)) {
                     case VCConstants.HOST_CPU_GROUP + VCConstants.HOST_METRIC_USAGE:
                        valueUnit.setKey(MetricName.SERVER_CPUUSAGE);
                        valueUnit.setValueNum(value / 100.0);
                        valueUnit.setUnit(ValueUnit.MetricUnit.percent.name());
                        break;
                     case VCConstants.HOST_CPU_GROUP + VCConstants.HOST_METRIC_USAGEMHZ:
                        valueUnit.setKey(MetricName.SERVER_CPUUSEDINMHZ);
                        valueUnit.setValueNum(value);
                        valueUnit.setUnit(ValueUnit.MetricUnit.Mhz.name());
                        break;
                     case VCConstants.HOST_MEMORY_GROUP + VCConstants.HOST_METRIC_USAGE:
                        valueUnit.setKey(MetricName.SERVER_MEMORYUSAGE);
                        valueUnit.setValueNum(value / 100.0);
                        valueUnit.setUnit(ValueUnit.MetricUnit.percent.name());
                        break;
                     case VCConstants.HOST_MEMORY_GROUP + VCConstants.HOST_METRIC_MEM_ACTIVE:
                        valueUnit.setKey(MetricName.SERVER_ACTIVEMEMORY);
                        valueUnit.setValueNum(value);
                        valueUnit.setUnit(ValueUnit.MetricUnit.kB.name());
                     break;
                     case VCConstants.HOST_MEMORY_GROUP + VCConstants.HOST_METRIC_MEM_SHARED:
                        valueUnit.setKey(MetricName.SERVER_SHAREDMEMORY);
                        valueUnit.setValueNum(value);
                        valueUnit.setUnit(ValueUnit.MetricUnit.kB.name());
                     break;
                     case VCConstants.HOST_MEMORY_GROUP + VCConstants.HOST_METRIC_MEM_CONSUMED:
                        valueUnit.setKey(MetricName.SERVER_CONSUMEDMEMORY);
                        valueUnit.setValueNum(value);
                        valueUnit.setUnit(ValueUnit.MetricUnit.kB.name());
                     break;
                     case VCConstants.HOST_MEMORY_GROUP + VCConstants.HOST_METRIC_MEM_SWAP:
                        valueUnit.setKey(MetricName.SERVER_SWAPMEMORY);
                        valueUnit.setValueNum(value);
                        valueUnit.setUnit(ValueUnit.MetricUnit.kB.name());
                     break;
                     case VCConstants.HOST_MEMORY_GROUP + VCConstants.HOST_METRIC_MEM_BALLON:
                        valueUnit.setKey(MetricName.SERVER_BALLOONMEMORY);
                        valueUnit.setValueNum(value);
                        valueUnit.setUnit(ValueUnit.MetricUnit.kB.name());
                     break;
                     case VCConstants.HOST_DISK_GROUP + VCConstants.HOST_METRIC_USAGE:
                        valueUnit.setKey(MetricName.SERVER_STORAGEIORATEUSAGE);
                        valueUnit.setValueNum(value);
                        valueUnit.setUnit(ValueUnit.MetricUnit.kBps.name());
                     break;
                     case VCConstants.HOST_NETWORK_GROUP + VCConstants.HOST_METRIC_USAGE:
                        valueUnit.setKey(MetricName.SERVER_NETWORKUTILIZATION);
                        valueUnit.setValueNum(value);
                        valueUnit.setUnit(ValueUnit.MetricUnit.kBps.name());
                     break;
                     case VCConstants.HOST_POWER_GROUP + VCConstants.HOST_METRIC_POWER_POWER:
                        valueUnit.setKey(MetricName.SERVER_POWER);
                        valueUnit.setValueNum(valueUnit.translateUnit(value, ValueUnit.MetricUnit.W, ValueUnit.MetricUnit.kW));
                        valueUnit.setUnit(ValueUnit.MetricUnit.kW.name());
                        powerValueUnits.add(valueUnit);
                     break;
                     case VCConstants.HOST_POWER_GROUP + VCConstants.HOST_METRIC_POWER_ENERGY:
                        valueUnit.setKey(MetricName.SERVER_ENERGY_CONSUMPTION);
                        // This extraidentifier is the start time
                        valueUnit.setExtraidentifier(String.valueOf(timeStamp - (perfInterval * 1000)));
                        valueUnit.setValueNum(value * JOULETOKWHRATE);
                        valueUnit.setUnit(ValueUnit.MetricUnit.kWh.name());
                     break;
                  }
                  valueUnit.setTime(timeStamp);
                  valueUnits.add(valueUnit);
               }
            }
         }

      }
      if(valueUnits == null || valueUnits.isEmpty()) {
         logger.error("ValueUnits of asset: {} is empty.", assetId);
         return;
      }
      valueUnits.addAll(getMinMaxAvgValueUnit(powerValueUnits));
      realTimeData.setValues(valueUnits);
      realTimeData.setTime(valueUnits.get(0).getTime());
      realTimeDatas.add(realTimeData);

      restClient.saveRealTimeData(realTimeDatas);
   }

   public List<ValueUnit> getMinMaxAvgValueUnit (List<ValueUnit> valueUnits) {
      List<ValueUnit> statisticsValueUnit = new ArrayList<>(3);
      if (valueUnits == null || valueUnits.isEmpty()) {
         return statisticsValueUnit;
      }
      ValueUnit firstValueUnit = valueUnits.get(0);

      ValueUnit minValueUnit = new ValueUnit();
      minValueUnit.setUnit(firstValueUnit.getUnit());
      minValueUnit.setValueNum(firstValueUnit.getValueNum());
      minValueUnit.setTime(firstValueUnit.getTime());

      ValueUnit maxValueUnit = new ValueUnit();
      maxValueUnit.setUnit(firstValueUnit.getUnit());
      maxValueUnit.setValueNum(firstValueUnit.getValueNum());
      maxValueUnit.setTime(firstValueUnit.getTime());

      ValueUnit averageValueUnit = new ValueUnit();
      averageValueUnit.setUnit(firstValueUnit.getUnit());

      if (MetricName.SERVER_POWER.equals(firstValueUnit.getKey())) {
         minValueUnit.setKey(MetricName.SERVER_MINIMUM_USED_POWER);
         maxValueUnit.setKey(MetricName.SERVER_PEAK_USED_POWER);
         averageValueUnit.setKey(MetricName.SERVER_AVERAGE_USED_POWER);
      } else {
         return statisticsValueUnit;
      }
      double sum = 0.0d;
      long startTime = Long.MAX_VALUE;
      long endTime = Long.MIN_VALUE;
      for (ValueUnit value : valueUnits) {
         double valueNum = value.getValueNum();
         long time = value.getTime();
         sum += valueNum;
         if (valueNum < minValueUnit.getValueNum()) {
            minValueUnit.setValueNum(valueNum);
            minValueUnit.setTime(time);
         }
         if (valueNum > maxValueUnit.getValueNum()) {
            maxValueUnit.setValueNum(valueNum);
            maxValueUnit.setTime(time);
         }
         startTime = Math.min(time, startTime);
         endTime = Math.max(time, endTime);
      }
      // Record the since time and peak power time, for example  1612417403074_FIELDSPLIT_1612415606985
      minValueUnit.setExtraidentifier(startTime + FlowgateConstant.SEPARATOR + minValueUnit.getTime());
      minValueUnit.setTime(endTime);

      maxValueUnit.setExtraidentifier(startTime + FlowgateConstant.SEPARATOR + maxValueUnit.getTime());
      maxValueUnit.setTime(endTime);

      averageValueUnit.setValueNum(sum / valueUnits.size());
      averageValueUnit.setExtraidentifier(String.valueOf(startTime));
      averageValueUnit.setTime(endTime);

      statisticsValueUnit.add(minValueUnit);
      statisticsValueUnit.add(maxValueUnit);
      statisticsValueUnit.add(averageValueUnit);
      return statisticsValueUnit;
   }

   public boolean feedClusterMetaData(Map<String, ClusterComputeResource> clusterMap,
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

      try (VsphereClient vsphereClient = connectVsphere(vc)) {
         for (String key : VCConstants.hostCustomAttrMapping.values()) {
            vsphereClient.createCustomAttribute(key, VCConstants.HOSTSYSTEM);
         }
         // Add the PDU information;
         vsphereClient.createCustomAttribute(VCConstants.ASSET_PDUs, VCConstants.HOSTSYSTEM);
         // Add host switch information;
         vsphereClient.createCustomAttribute(VCConstants.ASSET_SWITCHs, VCConstants.HOSTSYSTEM);
      } catch (ConnectionException connectionException) {
         checkAndUpdateIntegrationStatus(vc, connectionException.getMessage());
         return;
      } catch (ExecutionException executionException) {
         if (executionException.getCause() instanceof InvalidLogin) {
            logger.error("Failed to push data to " + vc.getServerURL(), executionException);
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
      } catch (Exception exception) {
         logger.error("Failed to sync the host metadata to VC ", exception);
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
      } catch (Exception exception) {
         logger.error("Faild to check the predefined tag information", exception);
      }
   }

   private void syncCustomerAttrsData(SDDCSoftwareConfig vcInfo) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());

      try (VsphereClient vsphereClient = connectVsphere(vcInfo)) {
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
                           feedAssetMetricsFormulars(asset);
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
                        feedAssetMetricsFormulars(asset);
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
      } catch (ConnectionException connectionException) {
         checkAndUpdateIntegrationStatus(vcInfo, connectionException.getMessage());
         return;
      } catch (ExecutionException executionException) {
         if (executionException.getCause() instanceof InvalidLogin) {
            logger.error("Failed to push data to " + vcInfo.getServerURL(), executionException);
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
      } catch (Exception exception) {
         logger.error("Failed to push data to " + vcInfo.getServerURL(), exception);
      }
   }

   private void feedData(Map<String, Asset> assetDictionary, List<ServerMapping> validMapping,
         Map<String, HostSystem> hostDictionary) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      for (ServerMapping validServer : validMapping) {
         HostSystem host = hostDictionary.get(validServer.getVcMobID());
         Asset asset = assetDictionary.get(validServer.getAsset());
         feedAssetMetricsFormulars(asset);
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
            devicePorts.addAll(Arrays.asList(allPduPortString.split(FlowgateConstant.SPILIT_FLAG)));
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
         } catch (Exception exception) {
            logger.warn("Failed to tag the host, will try to tag it in next run.", exception);
         }
      }
   }

}
