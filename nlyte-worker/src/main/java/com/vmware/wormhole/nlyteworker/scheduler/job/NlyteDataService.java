/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.nlyteworker.scheduler.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.wormhole.client.WormholeAPIClient;
import com.vmware.wormhole.common.AssetCategory;
import com.vmware.wormhole.common.exception.WormholeException;
import com.vmware.wormhole.common.model.Asset;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.wormhole.common.model.RealTimeData;
import com.vmware.wormhole.common.model.ServerSensorData.ServerSensorType;
import com.vmware.wormhole.common.model.ValueUnit;
import com.vmware.wormhole.common.model.ValueUnit.MetricUnit;
import com.vmware.wormhole.common.model.ValueUnit.ValueType;
import com.vmware.wormhole.common.model.redis.message.AsyncService;
import com.vmware.wormhole.common.model.redis.message.EventMessage;
import com.vmware.wormhole.common.model.redis.message.EventType;
import com.vmware.wormhole.common.model.redis.message.EventUser;
import com.vmware.wormhole.common.model.redis.message.MessagePublisher;
import com.vmware.wormhole.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.wormhole.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.wormhole.common.utils.WormholeDateFormat;
import com.vmware.wormhole.nlyteworker.config.ServiceKeyConfig;
import com.vmware.wormhole.nlyteworker.model.JsonResultForPDURealtimeValue;
import com.vmware.wormhole.nlyteworker.model.LocationGroup;
import com.vmware.wormhole.nlyteworker.model.Manufacturer;
import com.vmware.wormhole.nlyteworker.model.Material;
import com.vmware.wormhole.nlyteworker.model.NlyteAsset;
import com.vmware.wormhole.nlyteworker.model.PowerStripsRealtimeValue;
import com.vmware.wormhole.nlyteworker.restclient.NlyteAPIClient;
import com.vmware.wormhole.nlyteworker.scheduler.job.common.HandleAssetUtil;

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
   private static final String DateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
   public static final String RealtimeLoad = "RealtimeLoad";
   public static final String RealtimePower = "RealtimePower";
   public static final String RealtimeVoltage = "RealtimeVoltage";
   public static Map<String,ValueType> sensorValueTypeMap =
         new HashMap<String,ValueType>();
   public static List<ServerSensorType> sensorType = new ArrayList<ServerSensorType>();
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

               for (EventUser payloadCommand : payloadMessage.getTarget().getUsers()) {
                  switch (payloadCommand.getId()) {
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

                     logger.info("Finish sync mapped data for " + nlyte.getName());
                  default:
                     break;
                  }
               }
            }
            break;
         case EventMessageUtil.NLYTE_SyncAllAssets:
            FacilitySoftwareConfig nlyteServer = null;
            try {
               nlyteServer = mapper.readValue(message.getContent(), FacilitySoftwareConfig.class);
            } catch (IOException e1) {
               // TODO Auto-generated catch block
               logger.error("Failed to convert message", e1);
            }
            if (nlyteServer != null) {
               logger.info("2-Sync all assets data for: " + nlyteServer.getName());
               SyncAlldata(nlyteServer);
               logger.info("2-Finish sync all assets data for: " + nlyteServer.getName());
            }
            //TODO send message to notify UI if needed.or notify a task system that this job is done.
            //now we do nothing.
            break;
         case EventMessageUtil.NLYTE_SyncRealtimeData:
            FacilitySoftwareConfig nlyte = null;
            try {
               nlyte = mapper.readValue(message.getContent(), FacilitySoftwareConfig.class);
            } catch (IOException e) {
               // TODO Auto-generated catch block
               logger.info("Failed to convert message", e);
            }
            if (nlyte != null) {
               logger.info("2-Sync realtime data for " + nlyte.getName());
               syncRealtimeData(nlyte);
               logger.info("2-Finish sync data for " + nlyte.getName());
            }
            break;
         case EventMessageUtil.NLYTE_SyncMappedAssetData:
            FacilitySoftwareConfig nlyteInfo = null;
            try {
               nlyteInfo = mapper.readValue(message.getContent(), FacilitySoftwareConfig.class);
            } catch (IOException e) {
               // TODO Auto-generated catch block
               logger.info("Failed to convert message", e);
            }
            if (nlyteInfo != null) {
               logger.info("2-Sync mapped data for " + nlyteInfo.getName());
               syncMappedData(nlyteInfo);
               logger.info("2-Finish sync mapped data for " + nlyteInfo.getName());
            }
            break;
         default:
            logger.warn("Not supported command");
            break;
         }
      }
   }

   private void SyncAlldata(FacilitySoftwareConfig nlyte) {
      NlyteAPIClient nlyteAPIclient = createClient(nlyte);
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      HandleAssetUtil assetUtil = new HandleAssetUtil();
      List<NlyteAsset> nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.Server);
      HashMap<Integer, LocationGroup> locationMap = assetUtil.initLocationGroupMap(nlyteAPIclient);
      HashMap<Integer, Manufacturer> manufacturerMap =
            assetUtil.initManufacturersMap(nlyteAPIclient);
      HashMap<Integer, Material> materialMap = assetUtil.initMaterialsMap(nlyteAPIclient);
      List<Asset> newServersNeedToSave = generateNewAsset(nlyte.getId(), nlyteAssets, locationMap,
            manufacturerMap, materialMap,AssetCategory.Server);
      restClient.saveAssets(newServersNeedToSave);

      HashMap<Integer, Material> pduMaterialMap = new HashMap<Integer, Material>();
      nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.PDU);
      List<Material> powerStripMaterials =
            nlyteAPIclient.getMaterials(true, HandleAssetUtil.powerStripMaterial);
      for (Material material : powerStripMaterials) {
         material.setMaterialType(AssetCategory.PDU);
         pduMaterialMap.put(material.getMaterialID(), material);
      }
      List<Asset> newPDUsNeedToSave = generateNewAsset(nlyte.getId(), nlyteAssets, locationMap,
            manufacturerMap, pduMaterialMap,AssetCategory.PDU);
      restClient.saveAssets(newPDUsNeedToSave);

      HashMap<Integer, Material> cabinetMaterialMap = new HashMap<Integer, Material>();
      nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.Cabinet);
      List<Material> cabinetMaterials =
            nlyteAPIclient.getMaterials(true, HandleAssetUtil.cabinetMaterials);
      for (Material material : cabinetMaterials) {
         material.setMaterialType(AssetCategory.Cabinet);
         cabinetMaterialMap.put(material.getMaterialID(), material);
      }
      List<Asset> newCabinetsNeedToSave = generateNewAsset(nlyte.getId(), nlyteAssets, locationMap,
            manufacturerMap, cabinetMaterialMap,AssetCategory.Cabinet);
      restClient.saveAssets(newCabinetsNeedToSave);
   }

   public List<Asset> generateNewAsset(String nlyteSource, List<NlyteAsset> nlyteAssets,
         HashMap<Integer, LocationGroup> locationMap,
         HashMap<Integer, Manufacturer> manufacturerMap, HashMap<Integer, Material> materialMap,
         AssetCategory category) {
      HandleAssetUtil assetUtil = new HandleAssetUtil();
      ResponseEntity<Asset[]> result = restClient.getAssetsBySourceAndType(nlyteSource, category);
      List<Asset> oldAssetsFromWormhole = null;
      if(result != null) {
         oldAssetsFromWormhole =  Arrays.asList(result.getBody());
      }

      Map<String, Asset> assetsFromWormholeMap = assetUtil.generateAssetsMap(oldAssetsFromWormhole);
      List<Asset> newAssetsFromNlyte = assetUtil.getAssetsFromNlyte(nlyteSource, nlyteAssets,
            locationMap, materialMap, manufacturerMap);
       return assetUtil.handleAssets(newAssetsFromNlyte, assetsFromWormholeMap);
   }

   public void saveAssetForMappedData(String nlyteSource, List<NlyteAsset> nlyteAssets,
         HashMap<Integer, LocationGroup> locationMap, HashMap<Integer, Material> materialMap,
         HashMap<Integer, Manufacturer> manufacturerMap, AssetCategory category) {
      HandleAssetUtil assetUtil = new HandleAssetUtil();
      List<Asset> allMappedAssets = Arrays.asList(restClient.getMappedAsset(category).getBody());
      if (allMappedAssets.isEmpty()) {
         return;
      }
      assetUtil.filterAssetBySourceAndCategory(allMappedAssets, nlyteSource, category);
      List<Asset> assetsFromNlyte = assetUtil.getAssetsFromNlyte(nlyteSource, nlyteAssets,
            locationMap, materialMap, manufacturerMap);
      Map<String, Asset> assetsFromNlyteMap = assetUtil.generateAssetsMap(assetsFromNlyte);
      List<Asset> newAssetsSaveToWormhole =
            assetUtil.handleAssets(allMappedAssets, assetsFromNlyteMap);
      restClient.saveAssets(newAssetsSaveToWormhole);
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
      if(realTimeDatas.isEmpty()) {
         logger.info("Not Found any data.");
         return;
      }
      logger.info("Found data size: " + realTimeDatas.size());
      restClient.saveRealTimeData(realTimeDatas);
   }

   public NlyteAPIClient createClient(FacilitySoftwareConfig config) {
      return new NlyteAPIClient(config);
   }

   public HashMap<AdvanceSettingType, String> getAdvanceSetting(FacilitySoftwareConfig nlyte){
      HashMap<AdvanceSettingType, String> advanceSettingMap = new HashMap<AdvanceSettingType, String>();
      if(nlyte.getAdvanceSetting() != null) {
         for(Map.Entry<AdvanceSettingType, String> map:nlyte.getAdvanceSetting().entrySet()) {
            if(map.getValue() != null) {
               advanceSettingMap.put(map.getKey(), map.getValue());
            }else {
               continue;
            }
         }
      }
      if(advanceSettingMap.get(AdvanceSettingType.DateFormat) == null) {
         advanceSettingMap.put(AdvanceSettingType.DateFormat, DateFormat);
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
         RealTimeData realTimeData = generateRealTimeData(asset, nlyteAPIclient,getAdvanceSetting(facilitySoftwareConfig));
         if(realTimeData == null) {
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
      List<ValueUnit> valueUnits = generateValueUnits(result.getValue(),advanceSettingMap);
      if(!valueUnits.isEmpty()) {
         realTimeData = new RealTimeData();
         realTimeData.setAssetID(asset.getId());
         realTimeData.setValues(valueUnits);
         realTimeData.setTime(valueUnits.get(0).getTime());
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
         EnumMap<ServerSensorType, String> sensorsformularsmap = asset.getSensorsformulars();
         for (Map.Entry<ServerSensorType, String> map : sensorsformularsmap.entrySet()) {
            if(sensorType.contains(map.getKey())) {
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
      for (PowerStripsRealtimeValue value : values) {
         String valueDateTime = value.getRecordedDateTime();
         long recordedTime = WormholeDateFormat.getLongTime(valueDateTime,dateFormat,timezone);
         if (recordedTime > currenttime || recordedTime == -1) {
            logger.error("Failed to translate the time string: " + valueDateTime);
            continue;
         }
         if(sensorValueTypeMap.containsKey(value.getName())) {
            ValueUnit valueunit = new ValueUnit();
            valueunit.setKey(sensorValueTypeMap.get(value.getName()));
            valueunit.setUnit(value.getUnit());
            String a = String.valueOf(value.getValue());

            MetricUnit targetUnit = null;
            switch(sensorValueTypeMap.get(value.getName())) {
                case PDU_RealtimeLoad:
                    targetUnit = MetricUnit.A;
                    break;
                case PDU_RealtimePower:
                    targetUnit = MetricUnit.KW;
                    break;
                case PDU_RealtimeVoltage:
                    targetUnit = MetricUnit.V;
                    break;
                default:
                    break;
            }
            try {
                valueunit.setValueNum(Double.parseDouble(valueunit.translateUnit(String.valueOf(value.getValue()), MetricUnit.valueOf(value.getUnit().toUpperCase()), targetUnit)));
           } catch (WormholeException e) {
               logger.error("Cannot translate Unit", e);
           }
            valueunit.setTime(recordedTime);
            valueunits.add(valueunit);
         }else {
            continue;
         }

      }
      return valueunits;
   }

   private void syncMappedData(FacilitySoftwareConfig nlyte) {
      NlyteAPIClient nlyteAPIclient = new NlyteAPIClient(nlyte);
      HandleAssetUtil assetUtil = new HandleAssetUtil();
      List<NlyteAsset> nlyteAssets = nlyteAPIclient.getAssets(true, AssetCategory.Server);
      HashMap<Integer, LocationGroup> locationMap = assetUtil.initLocationGroupMap(nlyteAPIclient);
      HashMap<Integer, Material> materialMap = assetUtil.initMaterialsMap(nlyteAPIclient);
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
   }
}
