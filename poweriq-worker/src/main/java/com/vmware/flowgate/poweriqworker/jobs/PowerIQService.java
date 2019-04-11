/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.jobs;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.poweriqworker.client.PowerIQAPIClient;
import com.vmware.flowgate.poweriqworker.config.ServiceKeyConfig;
import com.vmware.flowgate.poweriqworker.model.Aisle;
import com.vmware.flowgate.poweriqworker.model.DataCenter;
import com.vmware.flowgate.poweriqworker.model.Floor;
import com.vmware.flowgate.poweriqworker.model.InletReading;
import com.vmware.flowgate.poweriqworker.model.Parent;
import com.vmware.flowgate.poweriqworker.model.Pdu;
import com.vmware.flowgate.poweriqworker.model.Rack;
import com.vmware.flowgate.poweriqworker.model.Room;
import com.vmware.flowgate.poweriqworker.model.Row;
import com.vmware.flowgate.poweriqworker.model.Sensor;
import com.vmware.flowgate.poweriqworker.model.SensorReading;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.RealtimeDataUnit;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.ServerSensorData.ServerSensorType;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ValueUnit.MetricUnit;
import com.vmware.flowgate.common.model.ValueUnit.ValueType;
import com.vmware.flowgate.common.model.redis.message.AsyncService;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.EventUser;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.common.utils.WormholeDateFormat;

@Service
public class PowerIQService implements AsyncService {

   private static final Logger logger = LoggerFactory.getLogger(PowerIQService.class);

   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private StringRedisTemplate template;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;

   private ObjectMapper mapper = new ObjectMapper();
   
   private static final String DateFormat = "yyyy/MM/dd HH:mm:ss Z";
   public static final String rack_type = "rack";
   public static final String row_type = "row";
   public static final String aisle_type = "aisle";
   public static final String room_type = "room";
   public static final String floor_type = "floor";
   public static final String dataCenter_type = "data_center";
   public static final String HumiditySensor = "HumiditySensor";
   public static final String TemperatureSensor = "TemperatureSensor";
   public static final String AirFlowSensor = "AirFlowSensor";
   public static final String AirPressureSensor = "AirPressureSensor";
   public static final String ContactClosureSensor = "ContactClosureSensor";
   public static final String SmokeSensor = "SmokeSensor";
   public static final String WaterSensor = "WaterSensor";
   public static final String Vibration = "Vibration";
   private static final String Pdu_ID = "Pdu_ID";
   private static final String Sensor_ID = "Sensor_ID";
   private static Map<String, AssetSubCategory> subCategoryMap =
         new HashMap<String, AssetSubCategory>();
   public static List<ServerSensorType> sensorType = new ArrayList<ServerSensorType>();
   public static Map<String,ValueType> sensorValueType = new HashMap<String,ValueType>();
   static {
      subCategoryMap.put(HumiditySensor, AssetSubCategory.Humidity);
      subCategoryMap.put(TemperatureSensor, AssetSubCategory.Temperature);
      subCategoryMap.put(AirFlowSensor, AssetSubCategory.AirFlow);
      subCategoryMap.put(AirPressureSensor, AssetSubCategory.AirPressure);
      subCategoryMap.put(ContactClosureSensor, AssetSubCategory.ContactClosure);
      subCategoryMap.put(SmokeSensor, AssetSubCategory.Smoke);
      subCategoryMap.put(WaterSensor, AssetSubCategory.Water);
      subCategoryMap.put(Vibration, AssetSubCategory.Vibration);
      sensorType.add(ServerSensorType.HUMIDITY);
      sensorType.add(ServerSensorType.BACKPANELTEMP);
      sensorType.add(ServerSensorType.FRONTPANELTEMP);
      sensorValueType.put(HumiditySensor, ValueType.HUMIDITY);
      sensorValueType.put(TemperatureSensor, ValueType.TEMP);
      sensorValueType.put(AirFlowSensor, ValueType.AirFlow);
      sensorValueType.put(AirPressureSensor, ValueType.AirPressure);
      sensorValueType.put(ContactClosureSensor, ValueType.ContactClosure);
      sensorValueType.put(SmokeSensor, ValueType.Smoke);
      sensorValueType.put(WaterSensor, ValueType.Water);
      sensorValueType.put(Vibration, ValueType.Vibration);
   }

   @Override
   @Async("asyncServiceExecutor")
   public void executeAsync(EventMessage message) {
      //when receive message, will do the related jobs
      //Sync Power
      //update the value.
      if (message.getType() != EventType.PowerIQ) {
         logger.warn("Drop none PowerIQ message " + message.getType());
         return;
      }
      //TO, this should be comment out since it may contain vc password.
      logger.info("message received");
      Set<EventUser> users = message.getTarget().getUsers();

      for (EventUser command : users) {
         logger.info(command.getId());
         switch (command.getId()) {
         case EventMessageUtil.POWERIQ_SyncData:
            //it will sync all the data depend on the type in the vcjoblist.
            String messageString = null;
            while ((messageString =
                  template.opsForList().rightPop(EventMessageUtil.powerIQJobList)) != null) {
               EventMessage payloadMessage = null;
               try {
                  payloadMessage = mapper.readValue(messageString, EventMessageImpl.class);
               } catch (IOException e) {
                  logger.error("Cannot process message", e);
               }
               if (payloadMessage == null) {
                  continue;
               }
               FacilitySoftwareConfig powerIQinfo = null;
               try {
                  powerIQinfo =
                        mapper.readValue(payloadMessage.getContent(), FacilitySoftwareConfig.class);
               } catch (IOException e) {
                  logger.error("Cannot process message", e);
               }
               if (null == powerIQinfo) {
                  continue;
               }
               if(!powerIQinfo.checkIsActive()) {
                  continue;
               }
               for (EventUser payloadCommand : payloadMessage.getTarget().getUsers()) {
                  executeJob(payloadCommand.getId(),powerIQinfo);
               }
            }
            break;
         default:
            FacilitySoftwareConfig powerIQ = null;
            try {
               powerIQ = mapper.readValue(message.getContent(), FacilitySoftwareConfig.class);
            } catch (IOException e1) {
               // TODO Auto-generated catch block
               logger.error("Failed to convert message", e1);
            }
            if (powerIQ != null) {
               executeJob(command.getId(),powerIQ);
            }
            break;
         }
      }
   }
   
   private void executeJob(String commonId,FacilitySoftwareConfig powerIQ) {
      if(!powerIQ.checkIsActive()) {
         return;
      }
      switch (commonId) {
      case EventMessageUtil.PowerIQ_SyncSensorMetaData:
         logger.info("Sync Sensor metadata " + powerIQ.getName());
         syncSensorMetaData(powerIQ);
         logger.info("Finish sync Sensor metadata for: " + powerIQ.getName());
         break;
      case EventMessageUtil.PowerIQ_SyncRealtimeData:
         logger.info("Sync realtime data for " + powerIQ.getName());
         syncRealtimeData(powerIQ);
         syncSensorRealtimeData(powerIQ);
         logger.info("Finish sync realtime data for " + powerIQ.getName());
         break;
      default:
         logger.warn("Not supported command");
         break;
      }
   }
   
   private void updateIntegrationStatus(FacilitySoftwareConfig powerIQ) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      restClient.updateFacility(powerIQ);
   }

   public void syncSensorMetaData(FacilitySoftwareConfig powerIQ) {
      PowerIQAPIClient client = createClient(powerIQ);
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      List<Asset> sensorsFromPower = null;
     
      try {
         sensorsFromPower = getSensorMetaData(client, powerIQ.getId());
      }catch(ResourceAccessException e1) {
        if(e1.getCause().getCause() instanceof ConnectException) {
           checkAndUpdateIntegrationStatus(powerIQ,e1.getMessage());
           return;
        }
      }catch(HttpClientErrorException e) {
         logger.error("Failed to query data from PowerIQ", e);
         IntegrationStatus integrationStatus = powerIQ.getIntegrationStatus();
         if(integrationStatus == null) {
            integrationStatus = new IntegrationStatus();
         }
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(e.getMessage());
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         updateIntegrationStatus(powerIQ);
         return;
      }
      
      if (sensorsFromPower.isEmpty()) {
         return;
      }
      Map<String, Asset> exsitingSensorAssets = getAssetsFromWormhole(powerIQ.getId());
      List<Asset> assetsToSave = new ArrayList<Asset>();
      assetsToSave.addAll(filterAsset(exsitingSensorAssets, sensorsFromPower));
      if (assetsToSave.isEmpty()) {
         return;
      }
      restClient.saveAssets(assetsToSave);
   }
   
   public void checkAndUpdateIntegrationStatus(FacilitySoftwareConfig powerIQ,String message) {
      IntegrationStatus integrationStatus =  powerIQ.getIntegrationStatus();
      if(integrationStatus == null) {
         integrationStatus = new IntegrationStatus();
      }
      int timesOftry = integrationStatus.getRetryCounter();
      timesOftry++;
      if(timesOftry < FlowgateConstant.MAXNUMBEROFRETRIES) {
         integrationStatus.setRetryCounter(timesOftry);
      }else {
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(message);
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         logger.error("Failed to query data from PowerIQ,error message is "+message);
      }
      powerIQ.setIntegrationStatus(integrationStatus);
      updateIntegrationStatus(powerIQ);
   }

   public Map<Integer, Rack> getRacksMap(PowerIQAPIClient client) {
      Map<Integer, Rack> racksMap = new HashMap<Integer, Rack>();
      List<Rack> racks = new ArrayList<Rack>();
      try {
         racks = client.getRacks();
         if (racks != null && !racks.isEmpty()) {
            for (Rack rack : racks) {
               racksMap.put(rack.getId(), rack);
            }
         }
      } catch (Exception e) {
         logger.error("Faild to get Racks data.", e);
      }
      return racksMap;
   }

   public Map<Integer, Row> getRowsMap(PowerIQAPIClient client) {
      Map<Integer, Row> rowsMap = new HashMap<Integer, Row>();
      List<Row> rows = new ArrayList<Row>();
      try {
         rows = client.getRows();
         if (rows != null && !rows.isEmpty()) {
            for (Row row : rows) {
               rowsMap.put(row.getId(), row);
            }
         }
      } catch (Exception e) {
         logger.error("Faild to get Row information:", e);
      }
      return rowsMap;
   }

   public Map<Integer, Aisle> getAislesMap(PowerIQAPIClient client) {
      Map<Integer, Aisle> aislesMap = new HashMap<Integer, Aisle>();
      List<Aisle> aisles = new ArrayList<Aisle>();
      try {
         aisles = client.getAisles();
         if (aisles != null && !aisles.isEmpty()) {
            for (Aisle aisle : aisles) {
               aislesMap.put(aisle.getId(), aisle);
            }
         }
      } catch (Exception e) {
         logger.error("Faild to get Aisles Information:", e);
      }
      return aislesMap;
   }

   public Map<Integer, Room> getRoomsMap(PowerIQAPIClient client) {
      Map<Integer, Room> roomsMap = new HashMap<Integer, Room>();
      List<Room> rooms = new ArrayList<Room>();
      try {
         rooms = client.getRooms();
         if (rooms != null && !rooms.isEmpty()) {
            for (Room room : rooms) {
               roomsMap.put(room.getId(), room);
            }
         }
      } catch (Exception e) {
         logger.error("Faild to get Room information:", e);
      }
      return roomsMap;
   }

   public Map<Integer, Floor> getFloorsMap(PowerIQAPIClient client) {
      Map<Integer, Floor> floorsMap = new HashMap<Integer, Floor>();
      List<Floor> floors = new ArrayList<Floor>();
      try {
         floors = client.getFloors();
         if (floors != null && !floors.isEmpty()) {
            for (Floor floor : floors) {
               floorsMap.put(floor.getId(), floor);
            }
         }

      } catch (Exception e) {
         logger.error("Failed to get Floors information:", e);
      }
      return floorsMap;
   }

   public Map<Integer, DataCenter> getDataCentersMap(PowerIQAPIClient client) {
      Map<Integer, DataCenter> dataCentersMap = new HashMap<Integer, DataCenter>();
      List<DataCenter> dataCenters = new ArrayList<DataCenter>();
      try {
         dataCenters = client.getDataCenters();
         if (dataCenters != null && !dataCenters.isEmpty()) {
            for (DataCenter dataCenter : dataCenters) {
               dataCentersMap.put(dataCenter.getId(), dataCenter);
            }
         }
      } catch (Exception e) {
         logger.error("Failed to get DataCenters information", e);
      }
      return dataCentersMap;
   }

   public Map<String, Asset> getPDUAssetMap() {
      Map<String, Asset> assetsMap = new HashMap<String, Asset>();
      try {
         FacilitySoftwareConfig[] nlytes =
               restClient.getFacilitySoftwareByType(SoftwareType.Nlyte).getBody();
         if (nlytes == null || nlytes.length == 0) {
            return assetsMap;
         }
         for (FacilitySoftwareConfig nlyte : nlytes) {
            List<Asset> nlytePdus = Arrays.asList(
                  restClient.getAssetsBySourceAndType(nlyte.getId(), AssetCategory.PDU).getBody());
            for (Asset asset : nlytePdus) {
               assetsMap.put(asset.getAssetName().toLowerCase(), asset);
            }
         }

      } catch (Exception e) {
         logger.error("Failed to get PDU Maps", e);
      }
      return assetsMap;
   }

   public List<Asset> getSensorMetaData(PowerIQAPIClient client, String assetSource){
      List<Asset> assets = new ArrayList<Asset>();
      List<Asset> pdus = new ArrayList<Asset>();
      List<Sensor> sensors = getSensors(client);
      if (sensors == null || sensors.isEmpty()) {
         return assets;
      }
      Map<String, Asset> pduAssetMap = getPDUAssetMap();//get all pdus from flowgate
      Map<Integer, Rack> racksMap = getRacksMap(client);
      Map<Integer, Row> rowsMap = getRowsMap(client);
      Map<Integer, Aisle> aislesMap = getAislesMap(client);
      Map<Integer, Room> roomsMap = getRoomsMap(client);
      Map<Integer, Floor> floorsMap = getFloorsMap(client);
      Map<Integer, DataCenter> dataCentersMap = getDataCentersMap(client);

      Map<Integer, Pdu> pduMap = getPduMap(client);
      for (Sensor sensor : sensors) {
         Asset asset = null;
         HashMap<String, String> justificationfieldsForSensor = new HashMap<String, String>();
         if (sensor.getPduId() != null && !pduMap.isEmpty()) {
            asset = new Asset();
            Pdu pdu = pduMap.get(sensor.getPduId());
            Asset pduAsset = pduAssetMap.get(pdu.getName().toLowerCase());
            if (pduAsset == null) {
               asset = fillLocation(sensor, racksMap, rowsMap, aislesMap, roomsMap, floorsMap,
                     dataCentersMap);
            } else {
               //If the sensor's has pdu information. Then it can use the PDU's location info.
               asset.setRoom(pduAsset.getRoom());
               asset.setFloor(pduAsset.getFloor());
               asset.setBuilding(pduAsset.getBuilding());
               asset.setCity(pduAsset.getCity());
               asset.setCountry(pduAsset.getCountry());
               asset.setRegion(pduAsset.getRegion());
               //Record the pdu_assetId for the sensor.
               justificationfieldsForSensor.put(FlowgateConstant.PDU_ASSET_ID, pduAsset.getId());
               //Record the sensorId and sensor_source for the pdu.
               pduAsset = aggregatorSensorIdAndSourceForPdu(pduAsset,sensor,assetSource);
               pdus.add(pduAsset);
            }
         } else {
            asset = fillLocation(sensor, racksMap, rowsMap, aislesMap, roomsMap, floorsMap,
                  dataCentersMap);
         }
         //the pduId and sensorId are form the powerIQ system.
         if(sensor.getPduId() != null) {
            justificationfieldsForSensor.put(Pdu_ID, sensor.getPduId().toString());
         }
         justificationfieldsForSensor.put(Sensor_ID, sensor.getId()+"");
         asset.setAssetName(sensor.getName());
         asset.setJustificationfields(justificationfieldsForSensor);
         asset.setSerialnumber(sensor.getSerialNumber());
         asset.setAssetSource(assetSource);
         asset.setCategory(AssetCategory.Sensors);
         asset.setSubCategory(subCategoryMap.get(sensor.getType()));
         asset.setCreated(System.currentTimeMillis());
         assets.add(asset);
      }
      restClient.saveAssets(pdus);//save pdus 
      return assets;
   }
   
   //Record the sensorId and sensor source for the pdu.
   public Asset aggregatorSensorIdAndSourceForPdu(Asset pduAsset,Sensor sensor,String sensorSource) {
      HashMap<String, String> justificationfields = pduAsset.getJustificationfields();
      if(justificationfields == null) {
         justificationfields = new HashMap<String, String>();
         justificationfields.put(subCategoryMap.get(sensor.getType()).toString(), 
               sensor.getId()+FlowgateConstant.SEPARATOR+sensorSource);
      }else {
         String sensorIdAndSource = justificationfields.get(subCategoryMap.get(sensor.getType()).toString());
         if(sensorIdAndSource != null) {
            String [] existedSensor = sensorIdAndSource.split(FlowgateConstant.SPILIT_FLAG);
            Set<String> sensorIdAndSourceSet = new HashSet<String>();
            Collections.addAll(sensorIdAndSourceSet, existedSensor);
            sensorIdAndSourceSet.add(sensor.getId()+FlowgateConstant.SEPARATOR+sensorSource);
            sensorIdAndSource =  String.join(FlowgateConstant.SPILIT_FLAG, sensorIdAndSourceSet);;
         }else {
            sensorIdAndSource = sensor.getId()+FlowgateConstant.SEPARATOR+sensorSource;
         }
         justificationfields.put(subCategoryMap.get(sensor.getType()).toString(), sensorIdAndSource);
      }
      pduAsset.setJustificationfields(justificationfields);
      return pduAsset;
   }

   public List<Sensor> getSensors(PowerIQAPIClient client) {
      List<Sensor> sensors = new ArrayList<Sensor>();
      if (client != null) {
         sensors = client.getSensors();
      }
      return sensors;
   }

   public Map<Integer, Pdu> getPduMap(PowerIQAPIClient client) {
      Map<Integer, Pdu> pdusMap = new HashMap<Integer, Pdu>();
      List<Pdu> pdus = new ArrayList<Pdu>();
      try {
         pdus = client.getPdus();
         if (pdus != null && !pdus.isEmpty()) {
            for (Pdu pdu : pdus) {
               pdusMap.put(pdu.getId(), pdu);
            }
         }
      } catch (Exception e) {
         logger.error("Failed to get PDU map", e);
      }
      return pdusMap;
   }

   public Asset fillLocation(Sensor sensor, Map<Integer, Rack> racksMap, Map<Integer, Row> rowsMap,
         Map<Integer, Aisle> aislesMap, Map<Integer, Room> roomsMap, Map<Integer, Floor> floorsMap,
         Map<Integer, DataCenter> dataCentersMap) {
      Asset asset = null;
      StringBuilder extraLocation = new StringBuilder();
      Parent parent = null;
      parent = sensor.getParent();
      asset = new Asset();
      if (parent == null) {
         return asset;
      }
      boolean rackMapisEmpty = racksMap == null || racksMap.isEmpty();
      boolean rowMapisEmpty = rowsMap == null || rowsMap.isEmpty();
      boolean aislesMapIsEmpty = aislesMap == null || aislesMap.isEmpty();
      boolean roomsMapIsEmpty = roomsMap == null || roomsMap.isEmpty();
      boolean floorMapIsEmpty = floorsMap == null || floorsMap.isEmpty();
      boolean dataCenterMapIsEmpty = dataCentersMap == null || dataCentersMap.isEmpty();
      while (parent != null) {
         //TO enhancement, we should record the wrong items for later usage.
         if (parent.getType() == null) {
            parent = null;
            break;
         }
         switch (parent.getType()) {
         case rack_type:
            if (rackMapisEmpty) {
               parent = null;
               break;
            }
            Rack rack = racksMap.get(parent.getId());
            extraLocation.append(rack_type + ":" + rack.getName() + "" + ";");
            parent = rack.getParent();
            break;
         case row_type:
            if (rowMapisEmpty) {
               parent = null;
               break;
            }
            Row row = rowsMap.get(parent.getId());
            asset.setRow(row.getName());
            parent = row.getParent();
            break;
         case aisle_type:
            if (aislesMapIsEmpty) {
               parent = null;
               break;
            }
            Aisle ailse = aislesMap.get(parent.getId());
            extraLocation.append(aisle_type + ":" + ailse.getName() + "");
            parent = ailse.getParent();
            break;
         case room_type:
            if (roomsMapIsEmpty) {
               parent = null;
               break;
            }
            Room room = roomsMap.get(parent.getId());
            asset.setRoom(room.getName());
            parent = room.getParent();
            break;
         case floor_type:
            if (floorMapIsEmpty) {
               parent = null;
               break;
            }
            Floor floor = floorsMap.get(parent.getId());
            asset.setFloor(floor.getName());
            parent = floor.getParent();
            break;
         case dataCenter_type:
            if (dataCenterMapIsEmpty) {
               parent = null;
               break;
            }
            DataCenter dataCenter = dataCentersMap.get(parent.getId());
            asset.setCity(dataCenter.getCity());
            asset.setCountry(dataCenter.getCountry());
            parent = dataCenter.getParent();
            break;
         default:
            break;
         }
      }
      asset.setExtraLocation(extraLocation.toString());
      return asset;
   }

   //the sensorId is a unique value for each powerIQ system.
   public Map<String, Asset> getAssetsFromWormhole(String assetSource) {
      Map<String, Asset> sensors = new HashMap<String, Asset>();
      try {
         List<Asset> assets = Arrays.asList(
               restClient.getAssetsBySourceAndType(assetSource, AssetCategory.Sensors).getBody());
         for (Asset asset : assets) {
            String sensorId = asset.getJustificationfields().get(Sensor_ID);
            if(sensorId == null) {
               continue;
            }
            sensors.put(sensorId, asset);
         }
      } catch (Exception e) {
         logger.error("Get Sensors from Wormhole", e);
      }
      return sensors;
   }

   public List<Asset> filterAsset(Map<String, Asset> exsitingAsset, List<Asset> assetsFromPowerIQ) {
      if (assetsFromPowerIQ == null || assetsFromPowerIQ.isEmpty()) {
         return null;
      } else if (exsitingAsset == null || exsitingAsset.isEmpty()) {
         return assetsFromPowerIQ;
      }
      List<Asset> assets = new ArrayList<Asset>();
      for (Asset asset : assetsFromPowerIQ) {
         String sensorID = asset.getJustificationfields().get(Sensor_ID);
         if(sensorID == null) {
            continue;
         }
         Asset assetToUpdate =
               exsitingAsset.get(sensorID);
         if (assetToUpdate != null) {
            assetToUpdate.setAssetName(asset.getAssetName());
            assetToUpdate.setRow(asset.getRow());
            assetToUpdate.setRoom(asset.getRoom());
            assetToUpdate.setFloor(asset.getFloor());
            assetToUpdate.setBuilding(asset.getBuilding());
            assetToUpdate.setCity(asset.getCity());
            assetToUpdate.setCountry(asset.getCountry());
            assetToUpdate.setRegion(asset.getRegion());
            assetToUpdate.setSerialnumber(asset.getSerialnumber());
            HashMap<String, String> justificationfields = assetToUpdate.getJustificationfields();
            justificationfields.put(Pdu_ID, asset.getJustificationfields().get(Pdu_ID));
            justificationfields.put(Sensor_ID, asset.getJustificationfields().get(Sensor_ID));
            assetToUpdate.setJustificationfields(justificationfields);
            assetToUpdate.setLastupdate(System.currentTimeMillis());
            assets.add(assetToUpdate);
         } else {
            asset.setCreated(System.currentTimeMillis());
            assets.add(asset);
         }
      }
      return assets;
   }


   //Sync realtime Data logic need to refactor.
   private void syncRealtimeData(FacilitySoftwareConfig powerIQ) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      List<Asset> allMappedPdus =
            Arrays.asList(restClient.getMappedAsset(AssetCategory.PDU).getBody());
      if (allMappedPdus == null || allMappedPdus.isEmpty()) {
         return;
      }
      PowerIQAPIClient client = createClient(powerIQ);
      List<Pdu> pdus = null;
      try {
         pdus = client.getPdus();
      }catch(HttpClientErrorException e) {
         logger.error("Failed to query data from PowerIQ", e);
         IntegrationStatus integrationStatus = powerIQ.getIntegrationStatus();
         if(integrationStatus == null) {
            integrationStatus = new IntegrationStatus();
         }
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(e.getMessage());
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         updateIntegrationStatus(powerIQ);
         return;
      }catch(ResourceAccessException e1) {
         if(e1.getCause().getCause() instanceof ConnectException) {
            checkAndUpdateIntegrationStatus(powerIQ,e1.getMessage());
            return;
         }
       }
      Map<String, Pdu> pdusMap  = getPdusMapWithNameKey(pdus);//Map<pduName.lowcase,pdu>
      Map<String, Pdu> matchedPdus = getMatchedPdus(pdusMap, allMappedPdus);//Map<pduAssetId,pdu>
      List<RealTimeData> realTimeDatas = getRealTimeDatas(matchedPdus,getAdvanceSetting(powerIQ));
      if (!realTimeDatas.isEmpty()) {
         restClient.saveRealTimeData(realTimeDatas);
      }
   }
   
   public HashMap<AdvanceSettingType, String> getAdvanceSetting(FacilitySoftwareConfig powerIQ){
      HashMap<AdvanceSettingType, String> advanceSettingMap = new HashMap<AdvanceSettingType, String>();
      if(powerIQ.getAdvanceSetting() != null) {
         for(Map.Entry<AdvanceSettingType, String> map:powerIQ.getAdvanceSetting().entrySet()) {
            if(map.getValue() != null) {
               advanceSettingMap.put(map.getKey(), map.getValue());
            }else {
               continue;
            }
         }
      }
      String dateformat = advanceSettingMap.get(AdvanceSettingType.DateFormat);
      if(dateformat == null || dateformat.trim().isEmpty()) {
         advanceSettingMap.put(AdvanceSettingType.DateFormat, DateFormat);
      }
      if(advanceSettingMap.get(AdvanceSettingType.HUMIDITY_UNIT) == null || 
			  advanceSettingMap.get(AdvanceSettingType.HUMIDITY_UNIT).isEmpty()) {
	     advanceSettingMap.put(AdvanceSettingType.HUMIDITY_UNIT, MetricUnit.PERCENT.toString());
	  }
	  if(advanceSettingMap.get(AdvanceSettingType.PDU_AMPS_UNIT) == null || 
			  advanceSettingMap.get(AdvanceSettingType.PDU_AMPS_UNIT).isEmpty()) {
	     advanceSettingMap.put(AdvanceSettingType.PDU_AMPS_UNIT, MetricUnit.A.toString());
	  }
	  if(advanceSettingMap.get(AdvanceSettingType.PDU_POWER_UNIT) == null ||
			  advanceSettingMap.get(AdvanceSettingType.PDU_POWER_UNIT).isEmpty()) {
	     advanceSettingMap.put(AdvanceSettingType.PDU_POWER_UNIT, MetricUnit.KW.toString());
	  }
	  if(advanceSettingMap.get(AdvanceSettingType.PDU_VOLT_UNIT) == null || 
			  advanceSettingMap.get(AdvanceSettingType.PDU_VOLT_UNIT).isEmpty()) {
	     advanceSettingMap.put(AdvanceSettingType.PDU_VOLT_UNIT, MetricUnit.V.toString());
	  }
	  if(advanceSettingMap.get(AdvanceSettingType.TEMPERATURE_UNIT) == null || 
			  advanceSettingMap.get(AdvanceSettingType.TEMPERATURE_UNIT).isEmpty() ) {
	     advanceSettingMap.put(AdvanceSettingType.TEMPERATURE_UNIT, MetricUnit.C.toString());
	  }
      return advanceSettingMap;
   }

   public PowerIQAPIClient createClient(FacilitySoftwareConfig poweriq) {
      return new PowerIQAPIClient(poweriq);
   }

   public Map<String, Pdu> getPdusMapWithNameKey(List<Pdu> pdus) {
      Map<String, Pdu> pdusMap = new HashMap<String, Pdu>();
      for (Pdu pdu : pdus) {
         if (pdu.getName() != null) {
            pdusMap.put(pdu.getName().toLowerCase(), pdu);
         }
      }
      return pdusMap;
   }

   /**
    * If there are pdus what are from different PowerIQ have a same name,the method will have some
    * questions.
    *
    * @param filterCondition
    * @param pdusToFilter
    * @return
    */
   public Map<String, Pdu> getMatchedPdus(Map<String, Pdu> pduMapFromPoweIQ,
         List<Asset> mappedPDUsInWormhole) {
      Map<String, Pdu> matchedPDUs = new HashMap<String, Pdu>();
      if (pduMapFromPoweIQ == null || pduMapFromPoweIQ.isEmpty()) {
         return matchedPDUs;
      }
      for (Asset asset : mappedPDUsInWormhole) {
         Pdu pdu = pduMapFromPoweIQ.get(asset.getAssetName().toLowerCase());
         if (pdu != null) {
            matchedPDUs.put(asset.getId(), pdu);
         } else {
            continue;
         }
      }
      return matchedPDUs;
   }

   public List<RealTimeData> getRealTimeDatas(Map<String, Pdu> pdus,
         HashMap<AdvanceSettingType, String> advanceSettingMap) {
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      if (pdus == null || pdus.isEmpty()) {
         return realTimeDatas;
      }
      try {
         for (Map.Entry<String, Pdu> map : pdus.entrySet()) {
            List<ValueUnit> values = getValueUnits(map.getValue(),advanceSettingMap);
            if(!values.isEmpty()) {
               RealTimeData realTimeData = new RealTimeData();
               realTimeData.setAssetID(map.getKey());
               realTimeData.setValues(values);
               realTimeData.setTime(values.get(0).getTime());
               realTimeDatas.add(realTimeData);
            }else {
               continue;
            }
         }
      } catch (Exception e) {
         logger.error(
               "An exception occurred in the method of getRealTimeDatas from SyncPowerIQRealTimeDataJob",
               e);
      }
      return realTimeDatas;
   }

   public List<ValueUnit> getValueUnits(Pdu pdu,HashMap<AdvanceSettingType, String> advanceSettingMap) {
      List<ValueUnit> values = new ArrayList<ValueUnit>();
      if (pdu == null || pdu.getReading() == null || pdu.getReading().getInletReadings() == null) {
         return values;
      }
      double voltage = 0.0;
      double current = 0.0;
      double power = 0.0;
      String time = "";
      String dateFormat = advanceSettingMap.get(AdvanceSettingType.DateFormat);
      String timezone = advanceSettingMap.get(AdvanceSettingType.TimeZone);
      String PDU_AMPS_UNIT = advanceSettingMap.get(AdvanceSettingType.PDU_AMPS_UNIT);
      String PDU_POWER_UNIT = advanceSettingMap.get(AdvanceSettingType.PDU_POWER_UNIT);
      String PDU_VOLT_UNIT = advanceSettingMap.get(AdvanceSettingType.PDU_VOLT_UNIT);
      String TEMPERATURE_UNIT = advanceSettingMap.get(AdvanceSettingType.TEMPERATURE_UNIT);
      String HUMIDITY_UNIT = advanceSettingMap.get(AdvanceSettingType.HUMIDITY_UNIT);

      try {
         for (InletReading inletReading : pdu.getReading().getInletReadings()) {
            time = inletReading.getReadingTime();
            voltage = inletReading.getVoltage();
            current += inletReading.getCurrent();
            power += inletReading.getApparentPower();
         }
         
         long valueTime = WormholeDateFormat.getLongTime(time,dateFormat,timezone);
         if (valueTime == -1) {
            logger.error("Failed to translate the time string: " + time+".And the dateformat is "+dateFormat);
            return values;
         }
         ValueUnit voltageValue = new ValueUnit();
         voltageValue.setKey(ValueType.PDU_RealtimeVoltage);
         voltageValue.setTime(valueTime);
         voltageValue.setValueNum(Double.parseDouble(voltageValue.translateUnit(String.valueOf(voltage), MetricUnit.valueOf(PDU_VOLT_UNIT), MetricUnit.V)));
         voltageValue.setUnit(RealtimeDataUnit.Volts.toString());
         values.add(voltageValue);

         ValueUnit currentValue = new ValueUnit();
         currentValue.setKey(ValueType.PDU_RealtimeLoad);
         currentValue.setTime(valueTime);
         currentValue.setValueNum(Double.parseDouble(currentValue.translateUnit(String.valueOf(current), MetricUnit.valueOf(PDU_AMPS_UNIT), MetricUnit.A)));
         currentValue.setUnit(RealtimeDataUnit.Amps.toString());
         values.add(currentValue);
         
         if (pdu.getRatedAmps() != null) {
            ValueUnit currentValuePercent = new ValueUnit();
            currentValuePercent.setKey(ValueType.PDU_RealtimeLoadPercent);
            currentValuePercent.setTime(valueTime);
            currentValuePercent.setUnit(RealtimeDataUnit.Percent.toString());
            // "rated_amps": "32A",
            currentValuePercent.setValueNum(
                  current * 100.0 / Double.parseDouble(pdu.getRatedAmps().replaceAll("A", "")));
            values.add(currentValuePercent);
         }
         
         
         ValueUnit powerValue = new ValueUnit();
         powerValue.setKey(ValueType.PDU_RealtimePower);
         powerValue.setTime(valueTime);
         powerValue.setValueNum(Double.parseDouble(powerValue.translateUnit(String.valueOf(power), MetricUnit.valueOf(PDU_POWER_UNIT), MetricUnit.KW)));
         powerValue.setUnit(RealtimeDataUnit.KW.toString());
         values.add(powerValue);

         //ratedVA "rated_va": "6.4-7.7kVA"

      } catch (Exception e) {
         logger.error("Failed when generate the realtime data.", e);
      }
      return values;
   }

   public void syncSensorRealtimeData(FacilitySoftwareConfig powerIQ) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      List<Asset> allMappedAssets =
            Arrays.asList(restClient.getMappedAsset(AssetCategory.Server).getBody());
      if (allMappedAssets.isEmpty()) {
         logger.info("No mapped server found. End sync RealTime data Job");
         return;
      }
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      //filter and get assets which are from the PowerIQ
      List<Asset> mappedAssets = getPowerIQMappedAsset(allMappedAssets, powerIQ.getId());
      //get assetIds from asset's sensorsFromulars attribute
      Set<String> assetIds = getAssetIdfromformular(mappedAssets);
      if (assetIds.isEmpty()) {
         return;
      }
      realTimeDatas = getSensorRealTimeData(powerIQ, assetIds);
      logger.info("Received new Sensor data, data item size is:" + realTimeDatas.size());
      if(realTimeDatas.isEmpty()) {
         return;
      }
      restClient.saveRealTimeData(realTimeDatas);
   }
   
   public List<Asset> getPowerIQMappedAsset(List<Asset> allMappedAssets, String assetSource) {
      List<Asset> mappedAssets = new ArrayList<Asset>();
      for (Asset asset : allMappedAssets) {
         if (assetSource.equals(asset.getAssetSource())) {
            mappedAssets.add(asset);
         }
      }
      return mappedAssets;
   }

   public Set<String> getAssetIdfromformular(List<Asset> powerIQMappedAssets) {
      Set<String> assetIds = new HashSet<String>();
      for (Asset asset : powerIQMappedAssets) {
         EnumMap<ServerSensorType, String> sensorsformularsmap = asset.getSensorsformulars();
         for (Map.Entry<ServerSensorType, String> map : sensorsformularsmap.entrySet()) {
            if(sensorType.contains(map.getKey())) {
               String[] assetIDs = map.getValue().split("\\+|-|\\*|/|\\(|\\)");
               for (String assetId : assetIDs) {
                  if (assetId.equals("") || assetId.length() != FlowgateConstant.MONGOIDLENGTH) {
                     continue;
                  }
                  assetIds.add(assetId);
               }
            }
         }
      }
      return assetIds;
   }
   
   public List<RealTimeData> getSensorRealTimeData(FacilitySoftwareConfig powerIQ,Set<String> assetIds){
      HashMap<AdvanceSettingType, String> advanceSetting = getAdvanceSetting(powerIQ);
      List<RealTimeData> realtimeDatas = new ArrayList<RealTimeData>();
      String dateFormat = advanceSetting.get(AdvanceSettingType.DateFormat);
      String timezone = advanceSetting.get(AdvanceSettingType.TimeZone);
      String temperature = advanceSetting.get(AdvanceSettingType.TEMPERATURE_UNIT);
      String humidity = advanceSetting.get(AdvanceSettingType.HUMIDITY_UNIT);
      PowerIQAPIClient powerIQAPIClient = createClient(powerIQ);
      for(String assetId:assetIds) {
         Asset asset = restClient.getAssetByID(assetId).getBody();
         if(asset == null) {
            continue;
         }
         HashMap<String,String> sensorExtraInfo = asset.getJustificationfields();
         String sensorId =  sensorExtraInfo.get(Sensor_ID);
         Sensor sensor = null;
         try {
            sensor = powerIQAPIClient.getSensorById(sensorId);
         }
         catch(HttpClientErrorException e) {
            logger.error("Failed to query data from PowerIQ", e);
            IntegrationStatus integrationStatus = powerIQ.getIntegrationStatus();
            if(integrationStatus == null) {
               integrationStatus = new IntegrationStatus();
            }
            integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
            integrationStatus.setDetail(e.getMessage());
            integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
            updateIntegrationStatus(powerIQ);
            break;
         }catch(ResourceAccessException e1) {
            if(e1.getCause().getCause() instanceof ConnectException) {
               checkAndUpdateIntegrationStatus(powerIQ,e1.getMessage());
               break;
            }
            break;
          }
         SensorReading reading = sensor.getReading();
         if(reading == null) {
            continue;
         }
         RealTimeData realTimeData = new RealTimeData();
         String valueDateTime = reading.getReadingTime();
         long recordedTime = WormholeDateFormat.getLongTime(valueDateTime,dateFormat,timezone);
         if (recordedTime == -1) {
            logger.error("Failed to translate the time string: " + valueDateTime+".And the dateformat is "+dateFormat);
            continue;
         }
         List<ValueUnit> values = new ArrayList<ValueUnit>();
         ValueUnit value = new ValueUnit();
         value.setTime(recordedTime);
         value.setKey(sensorValueType.get(sensor.getType()));
         
         String unit = reading.getUom();
         MetricUnit sourceUnit = null, targetUnit = null;
         switch(sensorValueType.get(sensor.getType())) {
             case HUMIDITY:
            	 if(unit != null && !unit.isEmpty()) {
            		 if(unit.equals("%")) {
            			 sourceUnit = MetricUnit.PERCENT;
            		 }else {
            			 sourceUnit = MetricUnit.valueOf(unit.toUpperCase());
            		 }
            	 }else {
            		 if(humidity.equals("%")) {
            			 sourceUnit = MetricUnit.PERCENT;
            		 }else {
            			 sourceUnit = MetricUnit.valueOf(humidity.toUpperCase());
            		 }
            		 
            	 }
                 targetUnit = MetricUnit.PERCENT;
                 break;
             case TEMP:
            	 if(unit != null && !unit.isEmpty()) {
            		 sourceUnit = MetricUnit.valueOf(unit.toUpperCase());
            	 }else {
            		 sourceUnit = MetricUnit.valueOf(temperature.toUpperCase());
            	 }
                 targetUnit = MetricUnit.C;
                 break;
             default:
                 break;
         }
         
         try {
            value.setValueNum(Double.parseDouble(value.translateUnit(String.valueOf(reading.getValue()), sourceUnit, targetUnit)));
        } catch (WormholeException e) {
            logger.error("Cannot translate Unit", e);
        }
         if(targetUnit.toString().equals(MetricUnit.PERCENT.toString())) {
        	 value.setUnit("%");
         }else {
        	 value.setUnit(targetUnit.toString());
         }
         
         values.add(value);
         realTimeData.setAssetID(assetId);
         realTimeData.setTime(recordedTime);
         realTimeData.setValues(values);
         realtimeDatas.add(realTimeData);
      }
      return realtimeDatas;
   }
}
