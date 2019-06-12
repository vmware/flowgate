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
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.ServerSensorData.ServerSensorType;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ValueUnit.MetricUnit;
import com.vmware.flowgate.common.model.ValueUnit.ValueType;
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
   public static final String RealtimeLoad = "RealtimeLoad";
   public static final String RealtimePower = "RealtimePower";
   public static final String RealtimeVoltage = "RealtimeVoltage";
   public static Map<String, ValueType> sensorValueTypeMap = new HashMap<String, ValueType>();
   public static List<ServerSensorType> sensorType = new ArrayList<ServerSensorType>();
   public static long expiredTime = 30*24*3600*1000;//one month
   static {
      sensorType.add(ServerSensorType.PDU_RealtimeVoltage);
      sensorType.add(ServerSensorType.PDU_RealtimeLoad);
      sensorType.add(ServerSensorType.PDU_RealtimePower);
      sensorValueTypeMap.put(RealtimeLoad, ValueType.PDU_RealtimeLoad);
      sensorValueTypeMap.put(RealtimePower, ValueType.PDU_RealtimePower);
      sensorValueTypeMap.put(RealtimeVoltage, ValueType.PDU_RealtimeVoltage);
      sensorType = Collections.unmodifiableList(sensorType);
      sensorValueTypeMap = Collections.unmodifiableMap(sensorValueTypeMap);
   }

   @Override
   public void executeAsync(EventMessage message) {
      if (message.getType() != EventType.Nlyte) {
         logger.warn("Drop none Nlyte message " + message.getType());
         return;
      }
      //TO, this should be comment out since it may contain vc password.
      logger.info("message received");
      Set<EventUser> users = message.getTarget().getUsers();
      //IntegrationStatus integrationStatus = null;
      for (EventUser command : users) {
         logger.info(command.getId());
         switch (command.getId()) {
         case EventMessageUtil.NLYTE_SyncData:
            //it will sync all the data depend on the type in the vcjoblist.
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
      List<String> assetIds = new ArrayList<String>();

      List<Asset> servers = restClient.getAllAssetsBySourceAndType(nlyte.getId(), AssetCategory.Server);
      Map<Long,String> serverAssetNumberAndIdMap = getAssetNumberAndIdMap(servers);
      List<String> serverIds = filterAndGetAssetIds(serverAssetNumberAndIdMap, AssetCategory.Server, nlyteAPIclient);
      assetIds.addAll(serverIds);

      List<Asset> pdus = restClient.getAllAssetsBySourceAndType(nlyte.getId(), AssetCategory.PDU);
      Map<Long,String> pduAssetNumberAndIdMap = getAssetNumberAndIdMap(pdus);
      List<String> pduIds = filterAndGetAssetIds(pduAssetNumberAndIdMap, AssetCategory.PDU, nlyteAPIclient);
      assetIds.addAll(pduIds);

      List<Asset> networks = restClient.getAllAssetsBySourceAndType(nlyte.getId(), AssetCategory.Networks);
      Map<Long,String> networkAssetNumberAndIdMap = getAssetNumberAndIdMap(networks);
      List<String> switchIds = filterAndGetAssetIds(networkAssetNumberAndIdMap, AssetCategory.Networks, nlyteAPIclient);
      assetIds.addAll(switchIds);

      List<Asset> cabinets = restClient.getAllAssetsBySourceAndType(nlyte.getId(), AssetCategory.Cabinet);
      Map<Long,String> cabinetkAssetNumberAndIdMap = getAssetNumberAndIdMap(cabinets);
      assetIds.addAll(filterAndGetAssetIds(cabinetkAssetNumberAndIdMap, AssetCategory.Cabinet, nlyteAPIclient));

      //remove inactive pdus&networks information from server
      servers = cleanDependencyData(servers, pduIds, switchIds);
      restClient.saveAssets(servers);

      //get all serverMapping
      SDDCSoftwareConfig vcs[] = restClient.getVCServers().getBody();
      SDDCSoftwareConfig vros[] = restClient.getVROServers().getBody();
      List<ServerMapping> mappings = new ArrayList<ServerMapping>();
      for(SDDCSoftwareConfig vc : vcs) {
         mappings.addAll(new ArrayList<>(Arrays.asList(restClient.getServerMappingsByVC(vc.getId()).getBody())));
      }
      for(SDDCSoftwareConfig vro : vros) {
         mappings.addAll(new ArrayList<>(Arrays.asList(restClient.getServerMappingsByVRO(vro.getId()).getBody())));
      }
      //remove inactive asset from serverMapping
      for(ServerMapping mapping : mappings) {
         if(mapping.getAsset() == null) {
            continue;
         }
         if(serverIds.contains(mapping.getAsset())) {
            mapping.setAsset(null);
            restClient.saveServerMapping(mapping);
         }
      }
      //remove the inactive assets
      for(String assetid : assetIds) {
         restClient.removeAssetByID(assetid);
      }
   }

   public List<Asset> cleanDependencyData(List<Asset> servers, List<String> pdus, List<String> networks) {
      for (Asset server : servers) {
         server = updateJustficationfields(server,pdus,networks);
         server = updatePduAndSwitch(server,pdus,networks);
      }
      return servers;
   }

   public Asset updatePduAndSwitch(Asset server, List<String> pdus, List<String> networks) {
      List<String> pduIds = server.getPdus();
      List<String> switchIds = server.getSwitches();
      Iterator<String> pduite = pduIds.iterator();
      Iterator<String> switchite = switchIds.iterator();
      while(pduite.hasNext()) {
         String pduid = pduite.next();
         if(pdus.contains(pduid)) {
            pduite.remove();
         }
      }
      while(switchite.hasNext()) {
         String switchid = switchite.next();
         if(networks.contains(switchid)) {
            switchite.remove();
         }
      }
      server.setPdus(pduIds);
      server.setSwitches(switchIds);
      return server;
   }

   public Asset updateJustficationfields(Asset server, List<String> pdus, List<String> networks) {
      HashMap<String, String> serverJustficationfields = server.getJustificationfields();
      Set<String> pduDevices = new HashSet<String>();
      Set<String> networkDevices = new HashSet<String>();
      String pduPortString = serverJustficationfields.get(FlowgateConstant.PDU_PORT_FOR_SERVER);
      String networkPortString = serverJustficationfields.get(FlowgateConstant.NETWORK_PORT_FOR_SERVER);
      String pduPorts[] = pduPortString.split(FlowgateConstant.SPILIT_FLAG);
      String networkPorts[] = networkPortString.split(FlowgateConstant.SPILIT_FLAG);
      for(String pduport : pduPorts) {
         for(String pduid : pdus) {
            if(!pduport.contains(pduid)) {
               pduDevices.add(pduport);
            }
         }
      }
      for(String networkport : networkPorts) {
         for(String networkid : networks) {
            if(!networkport.contains(networkid)) {
               networkDevices.add(networkport);
            }
         }
      }
      pduPortString = String.join(FlowgateConstant.SPILIT_FLAG, pduDevices);
      networkPortString = String.join(FlowgateConstant.SPILIT_FLAG, networkDevices);
      serverJustficationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, pduPortString);
      serverJustficationfields.put(FlowgateConstant.NETWORK_PORT_FOR_SERVER, networkPortString);
      server.setJustificationfields(serverJustficationfields);
      return server;
   }

   public Map<Long,String> getAssetNumberAndIdMap(List<Asset> assets) {
      Map<Long,String> assetNumberAndIdMap = new HashMap<Long,String>();
      long currentTime = System.currentTimeMillis();
      long time = 0;
      for(Asset asset : assets) {
         long lastUpdateTime = asset.getLastupdate();
         long createTime = asset.getCreated();
         if(lastUpdateTime != 0) {
            time = lastUpdateTime;
         }else {
            time = createTime;
         }
         if(currentTime - time >= expiredTime) {
            assetNumberAndIdMap.put(asset.getAssetNumber(), asset.getId());
         }
      }
      return assetNumberAndIdMap;
   }

   public List<String> filterAndGetAssetIds(Map<Long,String> assetNumberAndIdMap, AssetCategory category,
         NlyteAPIClient nlyteAPIclient){
      List<String> assetIDs = new ArrayList<String>();
      NlyteAsset asset = null;
      for(Map.Entry<Long, String> map : assetNumberAndIdMap.entrySet()) {
         long assetnumber = map.getKey();
         asset = nlyteAPIclient.getAssetbyAssetNumber(category, assetnumber);
         if(asset == null) {
            assetIDs.add(map.getValue());
            logger.error("Invalid assetNumber: +"+map.getKey());
            continue;
         }
         if(!assetIsActived(asset, category)) {
            assetIDs.add(map.getValue());
         }
      }
      return assetIDs;
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
            manufacturerMap, cabinetMaterialMap, AssetCategory.Cabinet);
      restClient.saveAssets(cabinetsNeedToSaveOrUpdate);
      logger.info("Finish sync the cabinets data for: " + nlyte.getName());

      //init cabinetIdAndNameMap
      HashMap<Integer,String> cabinetIdAndNameMap = getCabinetIdAndNameMap(nlyteAssets);

      nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.Server);
      nlyteAssets = supplementCabinetName(cabinetIdAndNameMap, nlyteAssets);
      HashMap<Integer, Material> materialMap = assetUtil.initServerMaterialsMap(nlyteAPIclient);
      List<Asset> serversNeedToSaveOrUpdate = generateAssets(nlyte.getId(), nlyteAssets, locationMap,
            manufacturerMap, materialMap, AssetCategory.Server);
      restClient.saveAssets(serversNeedToSaveOrUpdate);
      logger.info("Finish sync the servers data for: " + nlyte.getName());

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
            manufacturerMap, pduMaterialMap, AssetCategory.PDU);
      restClient.saveAssets(pDUsNeedToSaveOrUpdate);
      logger.info("Finish sync the pdus data for: " + nlyte.getName());



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
            locationMap, manufacturerMap, networkMaterialMap, AssetCategory.Networks);
      restClient.saveAssets(networkersNeedToSaveOrUpdate);
      logger.info("Finish sync the networks data for: " + nlyte.getName());
   }

   public List<Asset> generateAssets(String nlyteSource, List<NlyteAsset> nlyteAssets,
         HashMap<Integer, LocationGroup> locationMap,
         HashMap<Integer, Manufacturer> manufacturerMap, HashMap<Integer, Material> materialMap,
         AssetCategory category) {
      HandleAssetUtil assetUtil = new HandleAssetUtil();
      List<Asset> oldAssetsFromWormhole = restClient.getAllAssetsBySourceAndType(nlyteSource,category);
      Map<String, Asset> assetsFromWormholeMap = assetUtil.generateAssetsMap(oldAssetsFromWormhole);
      List<Asset> allAssetsFromNlyte = assetUtil.getAssetsFromNlyte(nlyteSource, nlyteAssets,
            locationMap, materialMap, manufacturerMap);
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
            locationMap, materialMap, manufacturerMap);
      Map<String, Asset> assetsFromNlyteMap = assetUtil.generateAssetsMap(assetsFromNlyte);
      List<Asset> updateAssets = new ArrayList<Asset>();
      for (Asset asset : allMappedAssets) {
         if (assetsFromNlyteMap
               .containsKey(asset.getAssetSource() + "_" + asset.getAssetNumber())) {
            Asset assetFromNlyte =
                  assetsFromNlyteMap.get(asset.getAssetSource() + "_" + asset.getAssetNumber());
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
            updateAssets.add(asset);
         }
      }
      if (!updateAssets.isEmpty()) {
         restClient.saveAssets(updateAssets);
      }
   }

   public void syncRealtimeData(FacilitySoftwareConfig nlyte) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      List<Asset> allMappedAssets =
            Arrays.asList(restClient.getMappedAsset(AssetCategory.Server).getBody());
      if (allMappedAssets.isEmpty()) {
         logger.info("No mapped server found. End sync RealTime data Job");
         return;
      }
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      //filter and get mapped assets what are from NLYTE
      List<Asset> mappedAssets = getNlyteMappedAsset(allMappedAssets, nlyte.getId());
      //get assetIds from asset's sensorsFromulars attribute
      Set<String> assetIds = getAssetIdfromformular(mappedAssets);
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
         advanceSettingMap.put(AdvanceSettingType.HUMIDITY_UNIT, MetricUnit.PERCENT.toString());
      }
      if (advanceSettingMap.get(AdvanceSettingType.PDU_AMPS_UNIT) == null
            || advanceSettingMap.get(AdvanceSettingType.PDU_AMPS_UNIT).isEmpty()) {
         advanceSettingMap.put(AdvanceSettingType.PDU_AMPS_UNIT, MetricUnit.A.toString());
      }
      if (advanceSettingMap.get(AdvanceSettingType.PDU_POWER_UNIT) == null
            || advanceSettingMap.get(AdvanceSettingType.PDU_POWER_UNIT).isEmpty()) {
         advanceSettingMap.put(AdvanceSettingType.PDU_POWER_UNIT, MetricUnit.KW.toString());
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

   public Set<String> getAssetIdfromformular(List<Asset> nlyteMappedAssets) {
      Set<String> assetIds = new HashSet<String>();
      for (Asset asset : nlyteMappedAssets) {
         Map<ServerSensorType, String> sensorsformularsmap = asset.getSensorsformulars();
         for (Map.Entry<ServerSensorType, String> map : sensorsformularsmap.entrySet()) {
            if (sensorType.contains(map.getKey())) {
               String[] assetIDs = map.getValue().split("\\+|-|\\*|/|\\(|\\)");
               for (String assetId : assetIDs) {
                  if (assetId.equals("")) {
                     continue;
                  }
                  assetIds.add(assetId);
               }
            }
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
            logger.error("Failed to translate the time string: " + valueDateTime);
            continue;
         }
         if (sensorValueTypeMap.containsKey(value.getName())) {
            ValueUnit valueunit = new ValueUnit();
            valueunit.setKey(sensorValueTypeMap.get(value.getName()));

            String unit = value.getUnit();

            MetricUnit targetUnit = null, sourceUnit = null;
            switch (sensorValueTypeMap.get(value.getName())) {
            case PDU_RealtimeLoad:
               if (unit != null && !unit.isEmpty()) {
                  sourceUnit = MetricUnit.valueOf(unit.toUpperCase());
               } else {
                  sourceUnit = MetricUnit.valueOf(current.toUpperCase());
               }
               targetUnit = MetricUnit.A;
               break;
            case PDU_RealtimePower:
               if (unit != null && !unit.isEmpty()) {
                  sourceUnit = MetricUnit.valueOf(unit.toUpperCase());
               } else {
                  sourceUnit = MetricUnit.valueOf(power.toUpperCase());
               }
               targetUnit = MetricUnit.KW;
               break;
            case PDU_RealtimeVoltage:
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
               valueunit.setValueNum(Double.parseDouble(valueunit
                     .translateUnit(String.valueOf(value.getValue()), sourceUnit, targetUnit)));
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
