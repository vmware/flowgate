/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vcworker.scheduler.job;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.web.client.ResourceAccessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.cis.tagging.CategoryModel;
import com.vmware.cis.tagging.CategoryModel.Cardinality;
import com.vmware.flowgate.vcworker.client.HostTagClient;
import com.vmware.flowgate.vcworker.client.VsphereClient;
import com.vmware.flowgate.vcworker.config.ServiceKeyConfig;
import com.vmware.flowgate.vcworker.model.VCConstants;
import com.vmware.cis.tagging.TagModel;
import com.vmware.vim.binding.vim.HostSystem;
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
                     syncHostMetaData(vcInfo);
                     logger.info("Finish sync data for " + vcInfo.getName());
                     break;
                  default:
                     break;
                  }
               }
            }
            break;
         case EventMessageUtil.VCENTER_SyncCustomerAttrs:
            SDDCSoftwareConfig vc = null;
            try {
               vc = mapper.readValue(message.getContent(), SDDCSoftwareConfig.class);
            } catch (IOException e1) {
               // TODO Auto-generated catch block
               logger.error("Failed to convert message", e1);
            }
            if (vc != null) {
               syncCustomAttributes(vc);
            }
            // TODO send message to notify UI if needed.or notify a task system that this
            // job is done.
            // now we do nothing.
            break;
         case EventMessageUtil.VCENTER_SyncCustomerAttrsData:
            SDDCSoftwareConfig vcInfo = null;
            try {
               vcInfo = mapper.readValue(message.getContent(), SDDCSoftwareConfig.class);
            } catch (IOException e) {
               // TODO Auto-generated catch block
               logger.info("Failed to convert message", e);
            }
            if (vcInfo != null) {
               syncHostMetaData(vcInfo);
            }
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

   private void syncCustomAttributes(SDDCSoftwareConfig vc) {
      // TODO need to allow only update 1 vcenter instead of all the vcenter.
      IntegrationStatus integrationStatus = vc.getIntegrationStatus();
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
      } catch(ResourceAccessException e1) {
         logger.error("Failed to sync the host metadata to VC ", e1);
         if(e1.getCause().getCause() instanceof ConnectException) {
            int timesOftry = integrationStatus.getRetryCounter();
            timesOftry++;
            if(timesOftry < FlowgateConstant.MAXNUMBEROFRETRIES) {
               integrationStatus.setRetryCounter(timesOftry);
            }else {
               integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
               integrationStatus.setDetail(e1.getMessage());
               integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
            }
            updateIntegrationStatus(vc);
            return;
         }
       }catch(HttpClientErrorException e) {
          logger.error("Failed to sync the host metadata to VC ", e);
          integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
          integrationStatus.setDetail(e.getMessage());
          integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
          updateIntegrationStatus(vc);
          return;
       }catch(Exception e) {
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

   private void syncHostMetaData(SDDCSoftwareConfig vcInfo) {
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
         Map<String, String> idNamePortMapping = new HashMap<String, String>();
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
            host.setCustomValue(VCConstants.ASSET_SERIALNUMBER, String.join(",", switchNameList));
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
