/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.scheduler.job;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.FlowgateChassisSlot;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ValueUnit.MetricUnit;
import com.vmware.flowgate.common.model.redis.message.AsyncService;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.EventUser;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.common.utils.WormholeDateFormat;
import com.vmware.flowgate.nlyteworker.config.ServiceKeyConfig;
import com.vmware.flowgate.nlyteworker.exception.NlyteWorkerException;
import com.vmware.flowgate.nlyteworker.model.JsonResultForPDURealtimeValue;
import com.vmware.flowgate.nlyteworker.model.LocationGroup;
import com.vmware.flowgate.nlyteworker.model.Manufacturer;
import com.vmware.flowgate.nlyteworker.model.Material;
import com.vmware.flowgate.nlyteworker.model.NlyteAsset;
import com.vmware.flowgate.nlyteworker.model.PowerStripsRealtimeValue;
import com.vmware.flowgate.nlyteworker.restclient.NlyteAPIClient;
import com.vmware.flowgate.nlyteworker.scheduler.job.common.HandleAssetUtil;

@Service
public class NlyteDataService implements AsyncService {
   private static final Logger logger = LoggerFactory.getLogger(NlyteDataService.class);

   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private MessagePublisher publisher;

   @Autowired
   private StringRedisTemplate template;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;
   private ObjectMapper mapper = new ObjectMapper();
   public static final String DateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
   private static final String RealtimeLoad = "RealtimeLoad";
   private static final String RealtimePower = "RealtimePower";
   private static final String RealtimeVoltage = "RealtimeVoltage";
   private static Map<String, String> sensorValueTypeMap = new HashMap<String, String>();
   static {
      sensorValueTypeMap.put(RealtimeLoad, MetricName.PDU_TOTAL_CURRENT);
      sensorValueTypeMap.put(RealtimePower, MetricName.PDU_TOTAL_POWER);
      sensorValueTypeMap.put(RealtimeVoltage, MetricName.PDU_VOLTAGE);
      sensorValueTypeMap = Collections.unmodifiableMap(sensorValueTypeMap);
   }

   @Override
   public void executeAsync(EventMessage message) {
      if (message.getType() != EventType.Nlyte) {
         logger.warn("Drop none Nlyte message " + message.getType());
         return;
      }
      logger.info("message received");
      Set<EventUser> users = message.getTarget().getUsers();
      for (EventUser command : users) {
         logger.info(command.getId());
         switch (command.getId()) {
         case EventMessageUtil.NLYTE_SyncData:
            //it will sync all the data depend on the type in the nlyteJobList.
            String messageString = null;
            while ((messageString =
                  template.opsForList().rightPop(EventMessageUtil.nlyteJobList)) != null) {
               EventMessage payloadMessage = null;
               try {
                  payloadMessage = mapper.readValue(messageString, EventMessageImpl.class);
               } catch (IOException e) {
                  logger.error("Cannot process message", e);
               }
               if (payloadMessage == null) {
                  continue;
               }
               FacilitySoftwareConfig nlyte = null;
               try {
                  nlyte =
                        mapper.readValue(payloadMessage.getContent(), FacilitySoftwareConfig.class);
               } catch (IOException e) {
                  logger.error("Cannot process message", e);
               }
               if (null == nlyte) {
                  continue;
               }
               if (!nlyte.checkIsActive()) {
                  continue;
               }
               for (EventUser payloadCommand : payloadMessage.getTarget().getUsers()) {
                  executeJob(payloadCommand.getId(), nlyte);
               }
            }
            break;
         default:
            FacilitySoftwareConfig nlyteInfo = null;
            try {
               nlyteInfo = mapper.readValue(message.getContent(), FacilitySoftwareConfig.class);
            } catch (IOException e) {
               // TODO Auto-generated catch block
               logger.info("Failed to convert message", e);
            }
            if (nlyteInfo != null) {
               executeJob(EventMessageUtil.NLYTE_SyncMappedAssetData, nlyteInfo);
            }
            break;
         }
      }
   }

   private void executeJob(String commonId, FacilitySoftwareConfig nlyte) {
      if (!nlyte.checkIsActive()) {
         return;
      }
      switch (commonId) {
      case EventMessageUtil.NLYTE_SyncAllAssets:
         logger.info("Sync all assets data for: " + nlyte.getName());
         SyncAlldata(nlyte);
         logger.info("Finish sync all assets data for: " + nlyte.getName());
         break;
      case EventMessageUtil.NLYTE_SyncRealtimeData:
         logger.info("Sync realtime data for " + nlyte.getName());
         syncRealtimeData(nlyte);
         logger.info("Finish sync data for " + nlyte.getName());
         break;
      case EventMessageUtil.NLYTE_SyncMappedAssetData:
         logger.info("Sync mapped data for " + nlyte.getName());
         syncMappedData(nlyte);
         logger.info("Finish sync mapped data for " + nlyte.getName());
         break;
      case EventMessageUtil.NLYTE_CleanInActiveAssetData:
         logger.info("Clean inactive data for " + nlyte.getName());
         removeInActiveData(nlyte);
         logger.info("Finish clean inactive data for " + nlyte.getName());
         break;
      default:
         logger.warn("Not supported command");
         break;
      }
   }

   private void updateIntegrationStatus(FacilitySoftwareConfig nlyte) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      restClient.updateFacility(nlyte);
   }

   private void removeInActiveData(FacilitySoftwareConfig nlyte) {
      NlyteAPIClient nlyteAPIclient = createClient(nlyte);
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      List<Asset> servers = restClient.getAllAssetsBySourceAndType(nlyte.getId(), AssetCategory.Server);

      List<Asset> pdus = restClient.getAllAssetsBySourceAndType(nlyte.getId(), AssetCategory.PDU);

      List<Asset> networks = restClient.getAllAssetsBySourceAndType(nlyte.getId(), AssetCategory.Networks);

      List<Asset> cabinets = restClient.getAllAssetsBySourceAndType(nlyte.getId(), AssetCategory.Cabinet);
      long currentTime = System.currentTimeMillis();
      //remove inactive pdus information from server and remove inactive pdus
      long expiredTimeRange = FlowgateConstant.DEFAULTEXPIREDTIMERANGE;
      String expiredTimeValue = template.opsForValue().get(EventMessageUtil.EXPIREDTIMERANGE);
      if(expiredTimeValue != null) {
         expiredTimeRange = Long.valueOf(expiredTimeValue);
      }
      for(Asset pdu : pdus) {
         if(!pdu.isExpired(currentTime, expiredTimeRange)) {
            continue;
         }
         NlyteAsset asset = nlyteAPIclient.getAssetbyAssetNumber(AssetCategory.PDU, pdu.getAssetNumber());
         if(asset == null || !assetIsActived(asset, AssetCategory.PDU)) {
            servers = removePduFromServer(servers, pdu.getId());
            restClient.saveAssets(servers);
            restClient.removeAssetByID(pdu.getId());
         }else if(asset != null && assetIsActived(asset, AssetCategory.PDU)) {
            pdu.setLastupdate(currentTime);
            restClient.saveAssets(pdu);
         }
      }

      //remove inactive network information from servers and remove inactive networks
      for(Asset network : networks) {
         if(!network.isExpired(currentTime, expiredTimeRange)) {
            continue;
         }
         NlyteAsset asset = nlyteAPIclient.getAssetbyAssetNumber(AssetCategory.Networks, network.getAssetNumber());
         if(asset == null || !assetIsActived(asset, AssetCategory.Networks)) {
            servers = removeNetworkFromServer(servers, network.getId());
            restClient.saveAssets(servers);
            restClient.removeAssetByID(network.getId());
         }else if(asset != null && assetIsActived(asset, AssetCategory.Networks)) {
            network.setLastupdate(currentTime);
            restClient.saveAssets(network);
         }
      }

      //remove cabinets
      for(Asset cabinet : cabinets) {
         if(!cabinet.isExpired(currentTime, expiredTimeRange)) {
            continue;
         }
         NlyteAsset asset = nlyteAPIclient.getAssetbyAssetNumber(AssetCategory.Cabinet, cabinet.getAssetNumber());
         if(asset == null || !assetIsActived(asset, AssetCategory.Cabinet)) {
            restClient.removeAssetByID(cabinet.getId());
         }else if(asset != null && assetIsActived(asset, AssetCategory.Cabinet)) {
            cabinet.setLastupdate(currentTime);
            restClient.saveAssets(cabinet);
         }
      }

      //get all serverMapping
      SDDCSoftwareConfig vcs[] = restClient.getInternalSDDCSoftwareConfigByType(SDDCSoftwareConfig.SoftwareType.VCENTER).getBody();
      SDDCSoftwareConfig vros[] = restClient.getInternalSDDCSoftwareConfigByType(SDDCSoftwareConfig.SoftwareType.VRO).getBody();
      List<ServerMapping> mappings = new ArrayList<ServerMapping>();
      for(SDDCSoftwareConfig vc : vcs) {
         mappings.addAll(new ArrayList<>(Arrays.asList(restClient.getServerMappingsByVC(vc.getId()).getBody())));
      }
      for(SDDCSoftwareConfig vro : vros) {
         mappings.addAll(new ArrayList<>(Arrays.asList(restClient.getServerMappingsByVRO(vro.getId()).getBody())));
      }

      //remove inactive asset from serverMapping and remove inactive servers
      for(Asset server : servers) {
         if(!server.isExpired(currentTime, expiredTimeRange)) {
            continue;
         }
         NlyteAsset asset = nlyteAPIclient.getAssetbyAssetNumber(AssetCategory.Server, server.getAssetNumber());
         if(asset == null || !assetIsActived(asset, AssetCategory.Server)) {
            for(ServerMapping mapping : mappings) {
               if(mapping.getAsset() == null) {
                  continue;
               }
               if(server.getId().equals(mapping.getAsset())) {
                  mapping.setAsset(null);
                  restClient.saveServerMapping(mapping);
               }
            }
            restClient.removeAssetByID(server.getId());
         }else if(asset != null && assetIsActived(asset, AssetCategory.Server)) {
            server.setLastupdate(currentTime);
            restClient.saveAssets(server);
         }
      }
   }

   public List<Asset> removePduFromServer(List<Asset> servers, String pduId) {
      List<Asset> needToUpdate = new ArrayList<Asset>();
      for(Asset server : servers) {
         boolean changed = false;
         HashMap<String, String> serverJustficationfields = server.getJustificationfields();
         Set<String> pduDevices = new HashSet<String>();
         String pduPortString = serverJustficationfields.get(FlowgateConstant.PDU_PORT_FOR_SERVER);
         if(pduPortString != null) {
            String pduPorts[] = pduPortString.split(FlowgateConstant.SPILIT_FLAG);
            for(String pduport : pduPorts) {
               if(!pduport.contains(pduId)) {
                  pduDevices.add(pduport);
                  changed = true;
               }
            }
            if(changed) {
               pduPortString = String.join(FlowgateConstant.SPILIT_FLAG, pduDevices);
               serverJustficationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, pduPortString);
               server.setJustificationfields(serverJustficationfields);
            }
         }
         List<String> pduIds = server.getPdus();
         Iterator<String> pduite = pduIds.iterator();
         while(pduite.hasNext()) {
            String pduid = pduite.next();
            if(pduid.equals(pduId)) {
               pduite.remove();
               changed = true;
            }
         }
         server.setPdus(pduIds);

         Map<String, String> formulars = server.getMetricsformulars();
         if(formulars == null || formulars.isEmpty()) {
            continue;
         }
         Map<String, Map<String, String>> pduFormulas = server.metricsFormulaToMap(formulars.get(FlowgateConstant.PDU), new TypeReference<Map<String, Map<String, String>>>() {});
         if (pduFormulas == null || pduFormulas.isEmpty()) {
            continue;
         }
         Iterator<Map.Entry<String, Map<String, String>>> ite = pduFormulas.entrySet().iterator();
         while(ite.hasNext()) {
            Map.Entry<String, Map<String, String>> map = ite.next();
            String pduAssetID = map.getKey();
            if (pduAssetID.equals(pduId)) {
               changed = true;
               ite.remove();
            }
         }

         formulars.put(FlowgateConstant.PDU, server.metricsFormulaToString(pduFormulas));
         server.setMetricsformulars(formulars);
         if(changed) {
            needToUpdate.add(server);
         }
      }
      return needToUpdate;
   }

   public List<Asset> removeNetworkFromServer(List<Asset> servers, String networkId) {
      List<Asset> needToUpdate = new ArrayList<Asset>();
      for(Asset server : servers) {
         boolean changed = false;
         HashMap<String, String> serverJustficationfields = server.getJustificationfields();
         Set<String> networkDevices = new HashSet<String>();
         String networkPortString = serverJustficationfields.get(FlowgateConstant.NETWORK_PORT_FOR_SERVER);
         if(networkPortString != null) {
            String networkPorts[] = networkPortString.split(FlowgateConstant.SPILIT_FLAG);
            for(String networkport : networkPorts) {
               if(!networkport.contains(networkId)) {
                  networkDevices.add(networkport);
                  changed = true;
               }
            }
            if(changed) {
               networkPortString = String.join(FlowgateConstant.SPILIT_FLAG, networkDevices);
               serverJustficationfields.put(FlowgateConstant.NETWORK_PORT_FOR_SERVER, networkPortString);
               server.setJustificationfields(serverJustficationfields);
            }
         }
         List<String> switchIds = server.getSwitches();
         Iterator<String> switchite = switchIds.iterator();
         while(switchite.hasNext()) {
            String switchid = switchite.next();
            if(switchid.equals(networkId)) {
               switchite.remove();
               changed = true;
            }
         }
         server.setSwitches(switchIds);
         if(changed) {
            needToUpdate.add(server);
         }
      }
      return needToUpdate;
   }

   public boolean assetIsActived(NlyteAsset nlyteAsset, AssetCategory category) {
      if (nlyteAsset.isTemplateRelated() || !nlyteAsset.isActived()) {
         return false;
      }
      switch (category) {
      case Server:
         if (nlyteAsset.getCabinetAssetID() <= 0) {
            return false;
         }
         return true;
      case PDU:
         if (nlyteAsset.getCabinetAssetID() <= 0 && nlyteAsset.getuMounting() == null) {
            return false;
         }
         return true;
      case Networks:
         if (nlyteAsset.getCabinetAssetID() <= 0) {
            return false;
         }
         return true;
      case Cabinet:
         return true;
      default:
         throw new NlyteWorkerException("Invalid category.");
      }
   }

   private void SyncAlldata(FacilitySoftwareConfig nlyte) {
      NlyteAPIClient nlyteAPIclient = createClient(nlyte);
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      HandleAssetUtil assetUtil = new HandleAssetUtil();
      List<NlyteAsset> nlyteAssets = null;
      try {
         nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.Cabinet);
      } catch (HttpClientErrorException e) {
         logger.error("Failed to query data from Nlyte", e);
         IntegrationStatus integrationStatus = nlyte.getIntegrationStatus();
         if (integrationStatus == null) {
            integrationStatus = new IntegrationStatus();
         }
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(e.getMessage());
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         updateIntegrationStatus(nlyte);
         return;
      } catch (ResourceAccessException e1) {
         if (e1.getCause().getCause() instanceof ConnectException) {
            checkAndUpdateIntegrationStatus(nlyte, e1.getMessage());
            return;
         }
      }
      HashMap<Long,String> chassisMountedAssetNumberAndChassisIdMap = null;
      HashMap<Integer, LocationGroup> locationMap = assetUtil.initLocationGroupMap(nlyteAPIclient);
      HashMap<Integer, Manufacturer> manufacturerMap = assetUtil.initManufacturersMap(nlyteAPIclient);
      HashMap<Integer, Material> cabinetMaterialMap = new HashMap<Integer, Material>();
      List<Material> cabinetMaterials =
            nlyteAPIclient.getMaterials(true, HandleAssetUtil.cabinetMaterials);
      for (Material material : cabinetMaterials) {
         material.setMaterialType(AssetCategory.Cabinet);
         cabinetMaterialMap.put(material.getMaterialID(), material);
      }
      List<Asset> cabinetsNeedToSaveOrUpdate = generateAssets(nlyte.getId(), nlyteAssets, locationMap,
            manufacturerMap, cabinetMaterialMap, AssetCategory.Cabinet, chassisMountedAssetNumberAndChassisIdMap);
      if(cabinetsNeedToSaveOrUpdate.isEmpty()) {
         logger.info("No cabinet asset need to save");
      }else {
         restClient.saveAssets(cabinetsNeedToSaveOrUpdate);
         logger.info("Finish sync the cabinets data for: " + nlyte.getName()+", size: " +cabinetsNeedToSaveOrUpdate.size());
      }

      //init cabinetIdAndNameMap
      HashMap<Integer,String> cabinetIdAndNameMap = getCabinetIdAndNameMap(nlyteAssets);

      nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.Chassis);
      nlyteAssets = supplementCabinetName(cabinetIdAndNameMap, nlyteAssets);
      List<Material> chassisMaterials =
            nlyteAPIclient.getMaterials(true, HandleAssetUtil.chassisMaterials);
      HashMap<Integer, Material> chassisMaterialMap = new HashMap<Integer, Material>();
      for (Material material : chassisMaterials) {
         material.setMaterialType(AssetCategory.Chassis);
         chassisMaterialMap.put(material.getMaterialID(), material);
      }
      List<Asset> chassisNeedToSaveOrUpdate = generateAssets(nlyte.getId(), nlyteAssets, locationMap,
            manufacturerMap, chassisMaterialMap, AssetCategory.Chassis, chassisMountedAssetNumberAndChassisIdMap);
      if(chassisNeedToSaveOrUpdate.isEmpty()) {
         logger.info("No chassis asset need to save");
      }else {
         restClient.saveAssets(chassisNeedToSaveOrUpdate);
         logger.info("Finish sync the chassis data for: " + nlyte.getName()+", size: "+chassisNeedToSaveOrUpdate.size());
      }

      List<Asset> chassisFromFlowgate = restClient.getAllAssetsBySourceAndType(nlyte.getId(), AssetCategory.Chassis);
      chassisMountedAssetNumberAndChassisIdMap = generateMountedAssetNumberAndChassisAssetIdMap(chassisFromFlowgate);

      nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.Server);
      nlyteAssets = supplementCabinetName(cabinetIdAndNameMap, nlyteAssets);
      HashMap<Integer, Material> materialMap = assetUtil.initServerMaterialsMap(nlyteAPIclient);
      List<Asset> serversNeedToSaveOrUpdate = generateAssets(nlyte.getId(), nlyteAssets, locationMap,
            manufacturerMap, materialMap, AssetCategory.Server,chassisMountedAssetNumberAndChassisIdMap);
      if(serversNeedToSaveOrUpdate.isEmpty()) {
         logger.info("No server asset need to save");
      }else {
         restClient.saveAssets(serversNeedToSaveOrUpdate);
         logger.info("Finish sync the servers data for: " + nlyte.getName()+", size: "+serversNeedToSaveOrUpdate.size());
      }

      HashMap<Integer, Material> pduMaterialMap = new HashMap<Integer, Material>();
      nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.PDU);
      nlyteAssets = supplementCabinetName(cabinetIdAndNameMap, nlyteAssets);
      List<Material> powerStripMaterials =
            nlyteAPIclient.getMaterials(true, HandleAssetUtil.powerStripMaterial);
      for (Material material : powerStripMaterials) {
         material.setMaterialType(AssetCategory.PDU);
         pduMaterialMap.put(material.getMaterialID(), material);
      }
      List<Asset> pDUsNeedToSaveOrUpdate = generateAssets(nlyte.getId(), nlyteAssets, locationMap,
            manufacturerMap, pduMaterialMap, AssetCategory.PDU, chassisMountedAssetNumberAndChassisIdMap);
      if(pDUsNeedToSaveOrUpdate.isEmpty()) {
         logger.info("No pdu asset need to save");
      }else {
         savePduAssetAndUpdatePduUsageFormula(pDUsNeedToSaveOrUpdate);
         logger.info("Finish sync the pdus data for: " + nlyte.getName()+", size: "+pDUsNeedToSaveOrUpdate.size());
      }

      HashMap<Integer, Material> networkMaterialMap = new HashMap<Integer, Material>();
      nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.Networks);
      nlyteAssets = supplementCabinetName(cabinetIdAndNameMap, nlyteAssets);
      List<Material> networkMaterials =
            nlyteAPIclient.getMaterials(true, HandleAssetUtil.networkMaterials);
      for (Material material : networkMaterials) {
         material.setMaterialType(AssetCategory.Networks);
         networkMaterialMap.put(material.getMaterialID(), material);
      }
      List<Asset> networkersNeedToSaveOrUpdate = generateAssets(nlyte.getId(), nlyteAssets,
            locationMap, manufacturerMap, networkMaterialMap, AssetCategory.Networks,chassisMountedAssetNumberAndChassisIdMap);
      if(networkersNeedToSaveOrUpdate.isEmpty()) {
         logger.info("No network asset need to save");
      }else {
         restClient.saveAssets(networkersNeedToSaveOrUpdate);
         logger.info("Finish sync the networks data for: " + nlyte.getName()+", size: "+networkersNeedToSaveOrUpdate.size());
      }

   }

   public void savePduAssetAndUpdatePduUsageFormula(List<Asset> pduList) {
      for (Asset pdu : pduList) {
         // save asset
         ResponseEntity<Void> responseEntity = restClient.saveAssets(pdu);
         if (responseEntity.getStatusCode().is2xxSuccessful()) {
            String pduId = getAssetIdByResponseEntity(responseEntity);
            pdu.setId(pduId);
            if (pdu.getMetricsformulars().get(FlowgateConstant.PDU) == null) {
               Map<String, String> pduMetricsFormulas = new HashMap<>(1);
               Map<String, String> pduUsageMetricsFormulas = new HashMap<>(3);
               pduUsageMetricsFormulas.put(MetricName.PDU_CURRENT, pduId);
               pduUsageMetricsFormulas.put(MetricName.PDU_TOTAL_POWER, pduId);
               pduUsageMetricsFormulas.put(MetricName.PDU_VOLTAGE, pduId);
               pduMetricsFormulas.put(FlowgateConstant.PDU, pdu.metricsFormulaToString(pduUsageMetricsFormulas));
               pdu.setMetricsformulars(pduMetricsFormulas);
               // save asset formula
               restClient.saveAssets(pdu);
            }
         }
      }
   }

   public String getAssetIdByResponseEntity(ResponseEntity<Void> ResponseEntity) {
      String uriPath = ResponseEntity.getHeaders().getLocation().getPath();
      return uriPath.substring(uriPath.lastIndexOf("/") + 1);
   }

   public HashMap<Long,String> generateMountedAssetNumberAndChassisAssetIdMap(List<Asset> chassisFromFlowgate){
      HashMap<Long,String> chassisMountedAssetNumberAndChassisIdMap = new HashMap<Long,String>();
      for(Asset asset : chassisFromFlowgate) {
         HashMap<String, String> justficationMap = asset.getJustificationfields();
         String chassisInfo = justficationMap.get(FlowgateConstant.CHASSIS);
         Map<String, String> chassisInfoMap = null;
         List<FlowgateChassisSlot> flowgateChassisSlots = null;
         if(chassisInfo != null) {
            try {
               chassisInfoMap = mapper.readValue(chassisInfo, new TypeReference<Map<String,String>>() {});
               String chassisSlots = chassisInfoMap.get(FlowgateConstant.CHASSISSLOTS);
               flowgateChassisSlots = mapper.readValue(chassisSlots, new TypeReference<List<FlowgateChassisSlot>>() {});
            } catch (Exception e) {
               logger.error("Failed to read the data of chassis slots, error: "+ e.getMessage());
               continue;
            }
            if(flowgateChassisSlots != null && !flowgateChassisSlots.isEmpty()) {
               for(FlowgateChassisSlot slot: flowgateChassisSlots) {
                  if(slot.getMountedAssetNumber() != null) {
                     chassisMountedAssetNumberAndChassisIdMap.put(slot.getMountedAssetNumber().longValue(), asset.getId());
                  }
               }
            }
         }else {
            continue;
         }
      }
      return chassisMountedAssetNumberAndChassisIdMap;
   }

   public List<Asset> generateAssets(String nlyteSource, List<NlyteAsset> nlyteAssets,
         HashMap<Integer, LocationGroup> locationMap,
         HashMap<Integer, Manufacturer> manufacturerMap, HashMap<Integer, Material> materialMap,
         AssetCategory category,HashMap<Long,String> chassisMountedAssetNumberAndChassisIdMap) {
      HandleAssetUtil assetUtil = new HandleAssetUtil();
      List<Asset> oldAssetsFromWormhole = restClient.getAllAssetsBySourceAndType(nlyteSource,category);
      Map<Long, Asset> assetsFromWormholeMap = assetUtil.generateAssetsMap(oldAssetsFromWormhole);
      List<Asset> allAssetsFromNlyte = assetUtil.getAssetsFromNlyte(nlyteSource, nlyteAssets,
            locationMap, materialMap, manufacturerMap, chassisMountedAssetNumberAndChassisIdMap);
      return assetUtil.handleAssets(allAssetsFromNlyte, assetsFromWormholeMap);
   }

   public void saveAssetForMappedData(String nlyteSource, List<NlyteAsset> nlyteAssets,
         HashMap<Integer, LocationGroup> locationMap, HashMap<Integer, Material> materialMap,
         HashMap<Integer, Manufacturer> manufacturerMap, AssetCategory category) {
      HandleAssetUtil assetUtil = new HandleAssetUtil();
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      List<Asset> allMappedAssets = Arrays.asList(restClient.getMappedAsset(category).getBody());
      if (allMappedAssets.isEmpty()) {
         return;
      }
      assetUtil.filterAssetBySourceAndCategory(allMappedAssets, nlyteSource, category);

      List<Asset> assetsFromNlyte = assetUtil.getAssetsFromNlyte(nlyteSource, nlyteAssets,
            locationMap, materialMap, manufacturerMap, null);
      Map<Long, Asset> assetsFromNlyteMap = assetUtil.generateAssetsMap(assetsFromNlyte);
      List<Asset> updateAssets = new ArrayList<Asset>();
      for (Asset asset : allMappedAssets) {
         if (assetsFromNlyteMap
               .containsKey(asset.getAssetNumber())) {
            Asset assetFromNlyte =
                  assetsFromNlyteMap.get(asset.getAssetNumber());
            asset.setTag(assetFromNlyte.getTag());
            asset.setSerialnumber(assetFromNlyte.getSerialnumber());
            asset.setAssetName(assetFromNlyte.getAssetName());
            asset.setRegion(assetFromNlyte.getRegion());
            asset.setCountry(assetFromNlyte.getCountry());
            asset.setCity(assetFromNlyte.getCity());
            asset.setBuilding(assetFromNlyte.getBuilding());
            asset.setFloor(assetFromNlyte.getFloor());
            asset.setRoom(assetFromNlyte.getRoom());
            asset.setModel(assetFromNlyte.getModel());
            asset.setManufacturer(assetFromNlyte.getManufacturer());
            asset.setCategory(assetFromNlyte.getCategory());
            asset.setSubCategory(assetFromNlyte.getSubCategory());
            asset.setLastupdate(System.currentTimeMillis());
            asset.setMountingSide(assetFromNlyte.getMountingSide());
            asset.setTenant(assetFromNlyte.getTenant());
            updateAssets.add(asset);
         }
      }
      if (!updateAssets.isEmpty()) {
         restClient.saveAssets(updateAssets);
      }
   }

   public void syncRealtimeData(FacilitySoftwareConfig nlyte) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      List<Asset> allMappedServers =
            Arrays.asList(restClient.getMappedAsset(AssetCategory.Server).getBody());
      if (allMappedServers.isEmpty()) {
         logger.info("No mapped server found. End sync RealTime data Job");
         return;
      }
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      //filter and get mapped assets what are from NLYTE
      List<Asset> mappedServersFromNlyte = getNlyteMappedAsset(allMappedServers, nlyte.getId());
      //get assetIds from asset's sensorsFromulars attribute
      Set<String> assetIds = getAssetIdfromformular(mappedServersFromNlyte);
      if (assetIds.isEmpty()) {
         return;
      }
      realTimeDatas = getRealTimeDatas(createClient(nlyte), nlyte, assetIds);
      if (realTimeDatas.isEmpty()) {
         logger.info("Not Found any data.");
         return;
      }
      logger.info("Found data size: " + realTimeDatas.size());
      restClient.saveRealTimeData(realTimeDatas);
   }

   public NlyteAPIClient createClient(FacilitySoftwareConfig config) {
      return new NlyteAPIClient(config);
   }

   public HashMap<AdvanceSettingType, String> getAdvanceSetting(FacilitySoftwareConfig nlyte) {
      HashMap<AdvanceSettingType, String> advanceSettingMap =
            new HashMap<AdvanceSettingType, String>();
      if (nlyte.getAdvanceSetting() != null) {
         for (Map.Entry<AdvanceSettingType, String> map : nlyte.getAdvanceSetting().entrySet()) {
            if (map.getValue() != null) {
               advanceSettingMap.put(map.getKey(), map.getValue());
            } else {
               continue;
            }
         }
      }
      String dateformat = advanceSettingMap.get(AdvanceSettingType.DateFormat);
      if (dateformat == null || dateformat.trim().isEmpty()) {
         advanceSettingMap.put(AdvanceSettingType.DateFormat, DateFormat);
      }
      if (advanceSettingMap.get(AdvanceSettingType.HUMIDITY_UNIT) == null
            || advanceSettingMap.get(AdvanceSettingType.HUMIDITY_UNIT).isEmpty()) {
         advanceSettingMap.put(AdvanceSettingType.HUMIDITY_UNIT, MetricUnit.percent.toString());
      }
      if (advanceSettingMap.get(AdvanceSettingType.PDU_AMPS_UNIT) == null
            || advanceSettingMap.get(AdvanceSettingType.PDU_AMPS_UNIT).isEmpty()) {
         advanceSettingMap.put(AdvanceSettingType.PDU_AMPS_UNIT, MetricUnit.A.toString());
      }
      if (advanceSettingMap.get(AdvanceSettingType.PDU_POWER_UNIT) == null
            || advanceSettingMap.get(AdvanceSettingType.PDU_POWER_UNIT).isEmpty()) {
         advanceSettingMap.put(AdvanceSettingType.PDU_POWER_UNIT, MetricUnit.kW.toString());
      }
      if (advanceSettingMap.get(AdvanceSettingType.PDU_VOLT_UNIT) == null
            || advanceSettingMap.get(AdvanceSettingType.PDU_VOLT_UNIT).isEmpty()) {
         advanceSettingMap.put(AdvanceSettingType.PDU_VOLT_UNIT, MetricUnit.V.toString());
      }
      if (advanceSettingMap.get(AdvanceSettingType.TEMPERATURE_UNIT) == null
            || advanceSettingMap.get(AdvanceSettingType.TEMPERATURE_UNIT).isEmpty()) {
         advanceSettingMap.put(AdvanceSettingType.TEMPERATURE_UNIT, MetricUnit.C.toString());
      }
      return advanceSettingMap;
   }

   public List<RealTimeData> getRealTimeDatas(NlyteAPIClient nlyteAPIclient,
         FacilitySoftwareConfig facilitySoftwareConfig, Set<String> assetIds) {
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      for (String assetId : assetIds) {
         Asset asset = restClient.getAssetByID(assetId).getBody();
         if (asset == null || !facilitySoftwareConfig.getId().equals(asset.getAssetSource())) {
            continue;
         }
         RealTimeData realTimeData = null;
         try {
            realTimeData = generateRealTimeData(asset, nlyteAPIclient,
                  getAdvanceSetting(facilitySoftwareConfig));
         } catch (HttpClientErrorException e) {
            logger.error("Failed to query data from Nlyte", e);
            IntegrationStatus integrationStatus = facilitySoftwareConfig.getIntegrationStatus();
            if (integrationStatus == null) {
               integrationStatus = new IntegrationStatus();
            }
            integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
            integrationStatus.setDetail(e.getMessage());
            integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
            updateIntegrationStatus(facilitySoftwareConfig);
            break;
         } catch (ResourceAccessException e1) {
            if (e1.getCause().getCause() instanceof ConnectException) {
               checkAndUpdateIntegrationStatus(facilitySoftwareConfig, e1.getMessage());
               break;
            }
         }
         if (realTimeData == null) {
            continue;
         }
         realTimeDatas.add(realTimeData);
      }
      return realTimeDatas;
   }

   public RealTimeData generateRealTimeData(Asset asset, NlyteAPIClient nlyteAPIclient,
         HashMap<AdvanceSettingType, String> advanceSettingMap) {
      RealTimeData realTimeData = null;
      JsonResultForPDURealtimeValue result =
            nlyteAPIclient.getPowerStripsRealtimeValue(asset.getAssetNumber()).getBody();
      List<ValueUnit> valueUnits = generateValueUnits(result.getValue(), advanceSettingMap);
      if (!valueUnits.isEmpty()) {
         realTimeData = new RealTimeData();
         realTimeData.setAssetID(asset.getId());
         realTimeData.setValues(valueUnits);
         realTimeData.setTime(valueUnits.get(0).getTime());
         realTimeData.setId(realTimeData.getAssetID() + "_" + realTimeData.getTime());
      }
      return realTimeData;
   }

   public List<Asset> getNlyteMappedAsset(List<Asset> allMappedAssets, String assetSource) {
      List<Asset> mappedAssets = new ArrayList<Asset>();
      for (Asset asset : allMappedAssets) {
         if (assetSource.equals(asset.getAssetSource())) {
            mappedAssets.add(asset);
         }
      }
      return mappedAssets;
   }

   public Set<String> getAssetIdfromformular(List<Asset> mappedServers) {
      Set<String> assetIds = new HashSet<String>();
      for (Asset asset : mappedServers) {
         Map<String, String> formulars = asset.getMetricsformulars();
         if(formulars == null || formulars.isEmpty()) {
            continue;
         }
         //{"pduAssetID",{"type_1","pduAssetID"}}
         Map<String, Map<String, String>> pduFormulas = asset.metricsFormulaToMap(formulars.get(FlowgateConstant.PDU), new TypeReference<Map<String, Map<String, String>>>() {});
         if(pduFormulas == null || pduFormulas.isEmpty()) {
            continue;
         }
         for(Map.Entry<String, Map<String, String>> pduFormularMap : pduFormulas.entrySet()) {
            assetIds.add(pduFormularMap.getKey());
         }
      }
      return assetIds;
   }

   public List<ValueUnit> generateValueUnits(List<PowerStripsRealtimeValue> values,
         HashMap<AdvanceSettingType, String> advanceSettingMap) {
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();
      long currenttime = System.currentTimeMillis();
      String dateFormat = advanceSettingMap.get(AdvanceSettingType.DateFormat);
      String timezone = advanceSettingMap.get(AdvanceSettingType.TimeZone);
      String current = advanceSettingMap.get(AdvanceSettingType.PDU_AMPS_UNIT);
      String power = advanceSettingMap.get(AdvanceSettingType.PDU_POWER_UNIT);
      String voltage = advanceSettingMap.get(AdvanceSettingType.PDU_VOLT_UNIT);

      for (PowerStripsRealtimeValue value : values) {
         String valueDateTime = value.getRecordedDateTime();
         long recordedTime = WormholeDateFormat.getLongTime(valueDateTime, dateFormat, timezone);
         if (recordedTime > currenttime || recordedTime == -1) {
            logger.error(
                  String.format("Failed to translate the time string: %s, current time is: %s",
                        valueDateTime, new Date()));
            continue;
         }
         if (sensorValueTypeMap.containsKey(value.getName())) {
            ValueUnit valueunit = new ValueUnit();
            valueunit.setKey(sensorValueTypeMap.get(value.getName()));

            String unit = value.getUnit();

            MetricUnit targetUnit = null, sourceUnit = null;
            switch (sensorValueTypeMap.get(value.getName())) {
            case MetricName.PDU_TOTAL_CURRENT:
               if (unit != null && !unit.isEmpty()) {
                  sourceUnit = MetricUnit.valueOf(unit.toUpperCase());
               } else {
                  sourceUnit = MetricUnit.valueOf(current.toUpperCase());
               }
               targetUnit = MetricUnit.A;
               break;
            case MetricName.PDU_TOTAL_POWER:
               if (unit != null && !unit.isEmpty()) {
                  sourceUnit = MetricUnit.valueOf(unit.toUpperCase());
               } else {
                  sourceUnit = MetricUnit.valueOf(power.toUpperCase());
               }
               targetUnit = MetricUnit.kW;
               break;
            case MetricName.PDU_VOLTAGE:
               if (unit != null && !unit.isEmpty()) {
                  sourceUnit = MetricUnit.valueOf(unit.toUpperCase());
               } else {
                  sourceUnit = MetricUnit.valueOf(voltage.toUpperCase());
               }
               targetUnit = MetricUnit.V;
               break;
            default:
               break;
            }
            valueunit.setUnit(targetUnit.toString());
            try {
               valueunit.setValueNum(valueunit
                     .translateUnit(value.getValue(), sourceUnit, targetUnit));
            } catch (WormholeException e) {
               logger.error("Cannot translate Unit", e);
            }
            valueunit.setTime(recordedTime);
            valueunits.add(valueunit);
         } else {
            continue;
         }

      }
      return valueunits;
   }

   public void checkAndUpdateIntegrationStatus(FacilitySoftwareConfig nlyte, String message) {
      IntegrationStatus integrationStatus = nlyte.getIntegrationStatus();
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
         logger.error("Failed to query data from Nlyte,error message is " + message);
      }
      nlyte.setIntegrationStatus(integrationStatus);
      updateIntegrationStatus(nlyte);
   }

   private void syncMappedData(FacilitySoftwareConfig nlyte) {
      NlyteAPIClient nlyteAPIclient = new NlyteAPIClient(nlyte);
      HandleAssetUtil assetUtil = new HandleAssetUtil();
      List<NlyteAsset> nlyteAssets = null;
      try {
         nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.Server);
      } catch (HttpClientErrorException e) {
         logger.error("Failed to query data from Nlyte", e);
         IntegrationStatus integrationStatus = nlyte.getIntegrationStatus();
         if (integrationStatus == null) {
            integrationStatus = new IntegrationStatus();
         }
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(e.getMessage());
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         updateIntegrationStatus(nlyte);
         return;
      } catch (ResourceAccessException e1) {
         if (e1.getCause().getCause() instanceof ConnectException) {
            checkAndUpdateIntegrationStatus(nlyte, e1.getMessage());
            return;
         }
      }
      HashMap<Integer, LocationGroup> locationMap = assetUtil.initLocationGroupMap(nlyteAPIclient);
      HashMap<Integer, Material> materialMap = assetUtil.initServerMaterialsMap(nlyteAPIclient);
      HashMap<Integer, Manufacturer> manufacturerMap =
            assetUtil.initManufacturersMap(nlyteAPIclient);
      saveAssetForMappedData(nlyte.getId(), nlyteAssets, locationMap, materialMap, manufacturerMap,
            AssetCategory.Server);

      HashMap<Integer, Material> pduMaterialMap = new HashMap<Integer, Material>();
      nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.PDU);
      List<Material> powerStripMaterials =
            nlyteAPIclient.getMaterials(true, HandleAssetUtil.powerStripMaterial);
      for (Material material : powerStripMaterials) {
         material.setMaterialType(AssetCategory.PDU);
         pduMaterialMap.put(material.getMaterialID(), material);
      }
      saveAssetForMappedData(nlyte.getId(), nlyteAssets, locationMap, pduMaterialMap,
            manufacturerMap, AssetCategory.PDU);

      HashMap<Integer, Material> networksMaterialMap = new HashMap<Integer, Material>();
      nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.Networks);
      List<Material> networkMaterials =
            nlyteAPIclient.getMaterials(true, HandleAssetUtil.networkMaterials);
      for (Material material : networkMaterials) {
         material.setMaterialType(AssetCategory.Networks);
         networksMaterialMap.put(material.getMaterialID(), material);
      }
      saveAssetForMappedData(nlyte.getId(), nlyteAssets, locationMap, networksMaterialMap,
            manufacturerMap, AssetCategory.Networks);
   }

   public HashMap<Integer,String> getCabinetIdAndNameMap(List<NlyteAsset> cabinets){
      HashMap<Integer,String> cabinetIdAndNameMap = new HashMap<Integer, String>();
      for(NlyteAsset cabinet:cabinets) {
         cabinetIdAndNameMap.put(cabinet.getAssetNumber(), cabinet.getAssetName());
      }
      return cabinetIdAndNameMap;
   }

   public List<NlyteAsset> supplementCabinetName(HashMap<Integer,String> cabinetIdAndNameMap, List<NlyteAsset> nlyteAssets){
      if(cabinetIdAndNameMap.isEmpty()) {
         return nlyteAssets;
      }
      for(NlyteAsset nlyteAsset:nlyteAssets) {
         if(nlyteAsset.getCabinetAssetID() > 0) {
            nlyteAsset.setCabinetName(cabinetIdAndNameMap.get(nlyteAsset.getCabinetAssetID()));
         }
      }
      return nlyteAssets;
   }
}
