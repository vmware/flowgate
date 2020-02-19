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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MountingSide;
import com.vmware.flowgate.common.RealtimeDataUnit;
import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.PduInlet;
import com.vmware.flowgate.common.model.PduOutlet;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ServerSensorData.ServerSensorType;
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
import com.vmware.flowgate.poweriqworker.client.PowerIQAPIClient;
import com.vmware.flowgate.poweriqworker.config.ServiceKeyConfig;
import com.vmware.flowgate.poweriqworker.model.Aisle;
import com.vmware.flowgate.poweriqworker.model.DataCenter;
import com.vmware.flowgate.poweriqworker.model.Floor;
import com.vmware.flowgate.poweriqworker.model.Inlet;
import com.vmware.flowgate.poweriqworker.model.InletReading;
import com.vmware.flowgate.poweriqworker.model.Outlet;
import com.vmware.flowgate.poweriqworker.model.Parent;
import com.vmware.flowgate.poweriqworker.model.Pdu;
import com.vmware.flowgate.poweriqworker.model.Rack;
import com.vmware.flowgate.poweriqworker.model.Room;
import com.vmware.flowgate.poweriqworker.model.Row;
import com.vmware.flowgate.poweriqworker.model.Sensor;
import com.vmware.flowgate.poweriqworker.model.SensorReading;

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
   private static final String POWERIQ_SOURCE = "PDU_PowerIQ_SOURCE";
   private static Map<String, AssetSubCategory> subCategoryMap =
         new HashMap<String, AssetSubCategory>();
   public static List<ServerSensorType> sensorType = new ArrayList<ServerSensorType>();
   public static Map<String, ValueType> sensorValueType = new HashMap<String, ValueType>();
   public static Map<AssetSubCategory, ServerSensorType> serverSensorTypeAndsubCategory =
         new HashMap<AssetSubCategory, ServerSensorType>();
   private static Map<String, MountingSide> sensorMountingSide =
         new HashMap<String, MountingSide>();
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
      sensorMountingSide.put("INLET", MountingSide.Front);
      sensorMountingSide.put("OUTLET", MountingSide.Back);
      sensorMountingSide.put("EXTERNAL", MountingSide.External);
      sensorMountingSide = Collections.unmodifiableMap(sensorMountingSide);
   }
   private static final String POWERIQ_POWER_UNIT = "kVA";
   private static final String POWERIQ_CURRENT_UNIT = "A";
   private static final String POWERIQ_VOLTAGE_UNIT = "V";
   private static final String RANGE_FLAG = "-";

   private Map<Long, Rack> racksMap;
   private Map<Long, Row> rowsMap;
   private Map<Long, Aisle> aislesMap;
   private Map<Long, Room> roomsMap;
   private Map<Long, Floor> floorsMap;
   private Map<Long, DataCenter> dataCentersMap;

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
               if (!powerIQinfo.checkIsActive()) {
                  continue;
               }
               for (EventUser payloadCommand : payloadMessage.getTarget().getUsers()) {
                  executeJob(payloadCommand.getId(), powerIQinfo);
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
               executeJob(command.getId(), powerIQ);
            }
            break;
         }
      }
   }

   private void executeJob(String commonId, FacilitySoftwareConfig powerIQ) {
      if (!powerIQ.checkIsActive()) {
         return;
      }
      switch (commonId) {
      case EventMessageUtil.PowerIQ_SyncAssetsMetaData:
         logger.info("Sync assets metadata from" + powerIQ.getName());
         syncPowerIQAssetsMetaData(powerIQ);
         logger.info("Finish sync assets metadata for: " + powerIQ.getName());
         break;
      case EventMessageUtil.PowerIQ_SyncRealtimeData:
         logger.info("Sync realtime data for " + powerIQ.getName());
         syncRealtimeData(powerIQ);
         syncSensorRealtimeData(powerIQ);
         logger.info("Finish sync realtime data for " + powerIQ.getName());
         break;
      case EventMessageUtil.PowerIQ_SyncAllPDUID:
         logger.info("Sync PDU ID for all PUDs");
         syncPDUID(powerIQ);
         logger.info("Finish sync PDU ID for all PDUs");
         break;
      case EventMessageUtil.PowerIQ_CleanInActiveAssetData:
         logger.info("Clean sensor metadata for " + powerIQ.getName());
         removeSensorMetaData(powerIQ);
         logger.info("Finish clean sensor metadata for " + powerIQ.getName());
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

   public void removeSensorMetaData(FacilitySoftwareConfig powerIQ) {
      PowerIQAPIClient client = createClient(powerIQ);
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      List<Asset> sensors = restClient.getAllAssetsBySourceAndType(powerIQ.getId(), AssetCategory.Sensors);
      List<Asset> servers = restClient.getAllAssetsByType(AssetCategory.Server);
      long currentTime = System.currentTimeMillis();
      String expiredTimeRangeValue = template.opsForValue().get(EventMessageUtil.EXPIREDTIMERANGE);
      long expiredTimeRange = 0l;
      if(expiredTimeRangeValue != null) {
         expiredTimeRange = Long.valueOf(expiredTimeRangeValue);
      }else {
         expiredTimeRange = FlowgateConstant.DEFAULTEXPIREDTIMERANGE;
      }
      for(Asset sensor : sensors) {
         if(!sensor.isExpired(currentTime, expiredTimeRange)) {
            continue;
         }
         Sensor sensorFromPowerIQ = client.getSensorById(String.valueOf(sensor.getAssetNumber()));
         if(sensorFromPowerIQ == null) {
            List<Asset> needToupdate = updateServer(servers, sensor.getId());
            restClient.saveAssets(needToupdate);
            restClient.removeAssetByID(sensor.getId());
         }else {
            sensor.setLastupdate(currentTime);
            restClient.saveAssets(sensor);
         }
      }
   }

   public List<Asset> updateServer(List<Asset> servers, String sensorId){
      List<Asset> needToUpdate = new ArrayList<Asset>();
      for(Asset server : servers) {
         boolean changed = false;
         Map<ServerSensorType,String> sensorsformular = server.getSensorsformulars();
         if(sensorsformular.isEmpty()) {
            continue;
         }
         String humidity = sensorsformular.get(ServerSensorType.HUMIDITY);
         if(humidity != null && humidity.indexOf(sensorId) >= 0) {
            sensorsformular.remove(ServerSensorType.HUMIDITY);
            changed = true;
         }
         String frontPanelTemp = sensorsformular.get(ServerSensorType.FRONTPANELTEMP);
         if(frontPanelTemp != null && frontPanelTemp.indexOf(sensorId) >= 0) {
            sensorsformular.remove(ServerSensorType.FRONTPANELTEMP);
            changed = true;
         }
         String backPanelTemp = sensorsformular.get(ServerSensorType.BACKPANELTEMP);
         if(backPanelTemp != null && backPanelTemp.indexOf(sensorId) >= 0) {
            sensorsformular.remove(ServerSensorType.BACKPANELTEMP);
            changed = true;
         }
         server.setSensorsformulars(sensorsformular);
         if(changed) {
            needToUpdate.add(server);
         }
      }
      return needToUpdate;
   }

   public void getLocationInfo(PowerIQAPIClient client) {
      this.aislesMap = getAislesMap(client);
      this.racksMap = getRacksMap(client);
      this.rowsMap = getRowsMap(client);
      this.roomsMap = getRoomsMap(client);
      this.floorsMap =  getFloorsMap(client);
      this.dataCentersMap = getDataCentersMap(client);
   }

   public void syncPowerIQAssetsMetaData(FacilitySoftwareConfig powerIQ) {
      PowerIQAPIClient client = createClient(powerIQ);
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      try {
         client.testConnection();
      } catch (ResourceAccessException e1) {
         if (e1.getCause().getCause() instanceof ConnectException) {
            checkAndUpdateIntegrationStatus(powerIQ, e1.getMessage());
            return;
         }
      } catch (HttpClientErrorException e) {
         logger.error("Failed to query data from PowerIQ", e);
         IntegrationStatus integrationStatus = powerIQ.getIntegrationStatus();
         if (integrationStatus == null) {
            integrationStatus = new IntegrationStatus();
         }
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(e.getMessage());
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         updateIntegrationStatus(powerIQ);
         return;
      }
      getLocationInfo(client);
      List<Asset> pdusFromFlowgate = restClient.getAllAssetsBySourceAndType(powerIQ.getId(), AssetCategory.PDU);
      Map<String, Asset> pduIDAndAssetMap = getPDUIDAndAssetMap(pdusFromFlowgate);

      savePduAssetsToFlowgate(pduIDAndAssetMap, powerIQ.getId(), client);
      logger.info("Finish sync PDU metadata for " + powerIQ.getName());

      Map<String, Asset> exsitingSensorAssets = getAssetsFromWormhole(powerIQ.getId());
      saveSensorAssetsToFlowgate(exsitingSensorAssets, client, powerIQ.getId(), pduIDAndAssetMap);
      logger.info("Finish sync Sensor metadata for: " + powerIQ.getName());
   }

   public void checkAndUpdateIntegrationStatus(FacilitySoftwareConfig powerIQ, String message) {
      IntegrationStatus integrationStatus = powerIQ.getIntegrationStatus();
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
         logger.error("Failed to query data from PowerIQ,error message is " + message);
      }
      powerIQ.setIntegrationStatus(integrationStatus);
      updateIntegrationStatus(powerIQ);
   }

   public Map<Long, Rack> getRacksMap(PowerIQAPIClient client) {
      Map<Long, Rack> racksMap = new HashMap<Long, Rack>();
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

   public Map<Long, Row> getRowsMap(PowerIQAPIClient client) {
      Map<Long, Row> rowsMap = new HashMap<Long, Row>();
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

   public Map<Long, Aisle> getAislesMap(PowerIQAPIClient client) {
      Map<Long, Aisle> aislesMap = new HashMap<Long, Aisle>();
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

   public Map<Long, Room> getRoomsMap(PowerIQAPIClient client) {
      Map<Long, Room> roomsMap = new HashMap<Long, Room>();
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

   public Map<Long, Floor> getFloorsMap(PowerIQAPIClient client) {
      Map<Long, Floor> floorsMap = new HashMap<Long, Floor>();
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

   public Map<Long, DataCenter> getDataCentersMap(PowerIQAPIClient client) {
      Map<Long, DataCenter> dataCentersMap = new HashMap<Long, DataCenter>();
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

   public void saveSensorAssetsToFlowgate(Map<String, Asset> exsitingSensorAssets,
         PowerIQAPIClient client, String assetSource, Map<String, Asset> pduAssetMap) {
      List<Sensor> sensors = null;
      int limit = 100;
      int offset = 0;
      List<Asset> assetsNeedToSave = null;
      while ((sensors = client.getSensors(limit, offset)) != null) {
         if (sensors.isEmpty()) {
            logger.warn(
                  String.format("No sensor data from PowerIQ %s", client.getPowerIQServiceEndpoint()));
            break;
         }
         assetsNeedToSave = new ArrayList<Asset>();
         for (Sensor sensor : sensors) {
            Asset asset = new Asset();
            Map<String,String> sensorMap = new HashMap<String,String>();
            sensorMap.put(FlowgateConstant.SENSOR_ID_FROM_POWERIQ, String.valueOf(sensor.getId()));
            if (sensor.getPduId() != null) {
               Asset pduAsset = pduAssetMap.get(String.valueOf(sensor.getPduId()));
               if (pduAsset == null) {
                  asset = fillLocation(sensor.getParent());
               } else {
                  //If the sensor's has pdu information. Then it can use the PDU's location info.
                  com.vmware.flowgate.common.model.Parent parent = new com.vmware.flowgate.common.model.Parent();
                  parent.setParentId(String.valueOf(sensor.getPduId()));
                  parent.setType(FlowgateConstant.PDU);
                  asset.setParent(parent);
                  asset.setRoom(pduAsset.getRoom());
                  asset.setFloor(pduAsset.getFloor());
                  asset.setBuilding(pduAsset.getBuilding());
                  asset.setCity(pduAsset.getCity());
                  asset.setCountry(pduAsset.getCountry());
                  asset.setRegion(pduAsset.getRegion());
                  //Record the pdu_assetId for the sensor.
                  sensorMap.put(FlowgateConstant.PDU_ASSET_ID, pduAsset.getId());
                  //Record the sensorId and sensor_source for the pdu.
                  pduAsset = aggregatorSensorIdAndSourceForPdu(pduAsset, sensor, assetSource);
                  restClient.saveAssets(pduAsset);
               }
            } else {
               asset = fillLocation(sensor.getParent());
            }

            Asset assetToUpdate = exsitingSensorAssets.get(String.valueOf(sensor.getId()));
            if (assetToUpdate != null) {
               assetToUpdate.setAssetName(sensor.getName());
               assetToUpdate.setRow(asset.getRow());
               assetToUpdate.setRoom(asset.getRoom());
               assetToUpdate.setFloor(asset.getFloor());
               assetToUpdate.setBuilding(asset.getBuilding());
               assetToUpdate.setCity(asset.getCity());
               assetToUpdate.setCountry(asset.getCountry());
               assetToUpdate.setRegion(asset.getRegion());
               assetToUpdate.setSerialnumber(sensor.getSerialNumber());
               assetToUpdate.setParent(asset.getParent());
               HashMap<String, String> oldjustificationfields = assetToUpdate.getJustificationfields();
               Map<String,String> oldSensorInfoMap = null;
               try {
                  oldSensorInfoMap = getInfoMap(oldjustificationfields.get(FlowgateConstant.SENSOR));
               } catch (IOException e) {
                  logger.error("Format sensor info map error", e);
               }
               if(oldSensorInfoMap != null) {
                  oldSensorInfoMap.put(FlowgateConstant.PDU_ASSET_ID, sensorMap.get(FlowgateConstant.PDU_ASSET_ID));
                  oldSensorInfoMap.put(FlowgateConstant.SENSOR_ID_FROM_POWERIQ, sensorMap.get(FlowgateConstant.SENSOR_ID_FROM_POWERIQ));
                  try {
                     String sensorInfo  = mapper.writeValueAsString(oldSensorInfoMap);
                     oldjustificationfields.put(FlowgateConstant.SENSOR, sensorInfo);
                  } catch (JsonProcessingException e) {
                     logger.error("Format sensor info map error", e);
                  }
               }
               assetToUpdate.setJustificationfields(oldjustificationfields);
               assetToUpdate.setLastupdate(System.currentTimeMillis());
               assetToUpdate.setMountingSide(sensorMountingSide.get(sensor.getPosition().toUpperCase()));
               //save
               assetsNeedToSave.add(assetToUpdate);
            } else {
               HashMap<String, String> justificationfieldsForSensor = new HashMap<String, String>();
               try {
                  justificationfieldsForSensor.put(FlowgateConstant.SENSOR, mapper.writeValueAsString(sensorMap));
               } catch (JsonProcessingException e) {
                  logger.error("Format sensor info map error", e);
               }
               asset.setAssetName(sensor.getName());
               asset.setJustificationfields(justificationfieldsForSensor);
               asset.setSerialnumber(sensor.getSerialNumber());
               asset.setAssetSource(assetSource);
               asset.setCategory(AssetCategory.Sensors);
               asset.setSubCategory(subCategoryMap.get(sensor.getType()));
               asset.setCreated(System.currentTimeMillis());
               if(sensor.getPosition() != null) {
                  asset.setMountingSide(sensorMountingSide.get(sensor.getPosition().toUpperCase()));
               }
               //save
               assetsNeedToSave.add(asset);
            }
         }
         restClient.saveAssets(assetsNeedToSave);
         offset += limit;
      }

   }

   //Record the sensorId and sensor source for the pdu.
   public Asset aggregatorSensorIdAndSourceForPdu(Asset pduAsset, Sensor sensor,
         String sensorSource) {
      HashMap<String, String> justificationfields = pduAsset.getJustificationfields();
      if (justificationfields == null) {
         justificationfields = new HashMap<String, String>();
         justificationfields.put(subCategoryMap.get(sensor.getType()).toString(),
               sensor.getId() + FlowgateConstant.SEPARATOR + sensorSource);
      } else {
         String sensorIdAndSource =
               justificationfields.get(subCategoryMap.get(sensor.getType()).toString());
         if (sensorIdAndSource != null) {
            String[] existedSensor = sensorIdAndSource.split(FlowgateConstant.SPILIT_FLAG);
            Set<String> sensorIdAndSourceSet = new HashSet<String>();
            Collections.addAll(sensorIdAndSourceSet, existedSensor);
            sensorIdAndSourceSet.add(sensor.getId() + FlowgateConstant.SEPARATOR + sensorSource);
            sensorIdAndSource = String.join(FlowgateConstant.SPILIT_FLAG, sensorIdAndSourceSet);
         } else {
            sensorIdAndSource = sensor.getId() + FlowgateConstant.SEPARATOR + sensorSource;
         }
         justificationfields.put(subCategoryMap.get(sensor.getType()).toString(),
               sensorIdAndSource);
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

   public Asset fillLocation(Parent parent) {
      Asset asset = null;
      StringBuilder extraLocation = new StringBuilder();
      asset = new Asset();
      if (parent == null) {
         return asset;
      }
      Map<Long, Rack> racksMap = this.racksMap;
      Map<Long, Row> rowsMap = this.rowsMap;
      Map<Long, Aisle> aislesMap = this.aislesMap;
      Map<Long, Room> roomsMap = this.roomsMap;
      Map<Long, Floor> floorsMap = this.floorsMap;
      Map<Long, DataCenter> dataCentersMap = this.dataCentersMap;

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
            logger.error(String.format("Unkown type %s", parent.getType()));
            parent = null;
            break;
         }
      }
      asset.setExtraLocation(extraLocation.toString());
      return asset;
   }

   //the sensorId is a unique value for each powerIQ system.
   public Map<String, Asset> getAssetsFromWormhole(String assetSource) {
      Map<String, Asset> sensors = new HashMap<String, Asset>();
      List<Asset> assets =
            restClient.getAllAssetsBySourceAndType(assetSource, AssetCategory.Sensors);
      for (Asset asset : assets) {
         HashMap<String,String> justficationfields = asset.getJustificationfields();
         if(justficationfields == null || justficationfields.isEmpty()) {
            continue;
         }
         String sensorInfo = justficationfields.get(FlowgateConstant.SENSOR);
         Map<String,String> sensorInfoMap = null;
         try {
            sensorInfoMap = getInfoMap(sensorInfo);
         } catch (IOException e) {
           continue;
         }
         String sensorId = sensorInfoMap.get(FlowgateConstant.SENSOR_ID_FROM_POWERIQ);
         if (sensorId == null) {
            continue;
         }
         sensors.put(sensorId, asset);
      }
      return sensors;
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
      try {
         client.testConnection();
      } catch (HttpClientErrorException e) {
         logger.error("Failed to query data from PowerIQ", e);
         IntegrationStatus integrationStatus = powerIQ.getIntegrationStatus();
         if (integrationStatus == null) {
            integrationStatus = new IntegrationStatus();
         }
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(e.getMessage());
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         updateIntegrationStatus(powerIQ);
         return;
      } catch (ResourceAccessException e1) {
         if (e1.getCause().getCause() instanceof ConnectException) {
            checkAndUpdateIntegrationStatus(powerIQ, e1.getMessage());
            return;
         }
      }
      Map<String, Pdu> matchedPdus = new HashMap<String, Pdu>();
      for (Asset asset : allMappedPdus) {
         String id = null;
         if (asset.getJustificationfields() != null) {
            HashMap<String,String> justficationfields = asset.getJustificationfields();
            String pduInfo = justficationfields.get(FlowgateConstant.PDU);
            Map<String,String> pduInfoMap = null;
            try {
              pduInfoMap = getInfoMap(pduInfo);
            } catch (IOException e) {
               continue;
            }
            id = pduInfoMap.get(FlowgateConstant.PDU_ID_FROM_POWERIQ);
            /**
             * In the future,the source will be stored in assetSource field,
             * and we only need to check if the assetSource contain it.
             */
            String source = asset.getJustificationfields().get(POWERIQ_SOURCE);
            //Only check the pdu that belong to the current PowerIQ.
            if (source == null || !source.equals(powerIQ.getId())) {
               id = null;
            }
         }
         if (id != null) {
            Pdu pdu = client.getPduByID(id);
            if (pdu != null) {
               matchedPdus.put(asset.getId(), pdu);
            }
         }
      }
      List<RealTimeData> realTimeDatas = getRealTimeDatas(matchedPdus, getAdvanceSetting(powerIQ));
      if (!realTimeDatas.isEmpty()) {
         restClient.saveRealTimeData(realTimeDatas);
      }
   }

   public HashMap<AdvanceSettingType, String> getAdvanceSetting(FacilitySoftwareConfig powerIQ) {
      HashMap<AdvanceSettingType, String> advanceSettingMap =
            new HashMap<AdvanceSettingType, String>();
      if (powerIQ.getAdvanceSetting() != null) {
         for (Map.Entry<AdvanceSettingType, String> map : powerIQ.getAdvanceSetting().entrySet()) {
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
            List<ValueUnit> values = getValueUnits(map.getValue(), advanceSettingMap);
            if (!values.isEmpty()) {
               RealTimeData realTimeData = new RealTimeData();
               realTimeData.setAssetID(map.getKey());
               realTimeData.setValues(values);
               realTimeData.setTime(values.get(0).getTime());
               //this will remove the duplicated items.
               realTimeData.setId(map.getKey() + "_" + realTimeData.getTime());
               realTimeDatas.add(realTimeData);
            } else {
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

   public List<ValueUnit> getValueUnits(Pdu pdu,
         HashMap<AdvanceSettingType, String> advanceSettingMap) {
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

         long valueTime = WormholeDateFormat.getLongTime(time, dateFormat, timezone);
         if (valueTime == -1) {
            logger.error("Failed to translate the time string: " + time + ".And the dateformat is "
                  + dateFormat);
            return values;
         }
         ValueUnit voltageValue = new ValueUnit();
         voltageValue.setKey(ValueType.PDU_RealtimeVoltage);
         voltageValue.setTime(valueTime);
         voltageValue
               .setValueNum(Double.parseDouble(voltageValue.translateUnit(String.valueOf(voltage),
                     MetricUnit.valueOf(PDU_VOLT_UNIT), MetricUnit.V)));
         voltageValue.setUnit(RealtimeDataUnit.Volts.toString());
         values.add(voltageValue);

         ValueUnit currentValue = new ValueUnit();
         currentValue.setKey(ValueType.PDU_RealtimeLoad);
         currentValue.setTime(valueTime);
         currentValue
               .setValueNum(Double.parseDouble(currentValue.translateUnit(String.valueOf(current),
                     MetricUnit.valueOf(PDU_AMPS_UNIT), MetricUnit.A)));
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
         powerValue.setValueNum(Double.parseDouble(powerValue.translateUnit(String.valueOf(power),
               MetricUnit.valueOf(PDU_POWER_UNIT), MetricUnit.KW)));
         powerValue.setUnit(RealtimeDataUnit.KW.toString());
         values.add(powerValue);

         //ratedVA "rated_va": "6.4-7.7kVA"

      } catch (Exception e) {
         logger.error("Failed when generate the realtime data.", e);
      }
      return values;
   }

   public List<Asset> filterAssetsBySource(String source, Set<String> assetIds) {
      List<Asset> assets = new ArrayList<Asset>();
      List<Asset> assetsFromPowerIQ = restClient.getAllAssetsBySource(source);
      for (Asset asset : assetsFromPowerIQ) {
         if (assetIds.contains(asset.getId())) {
            assets.add(asset);
         }
      }
      return assets;
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
      //get assetIds from asset's sensorsFromulars attribute
      Set<String> assetIds = getAssetIdfromformular(allMappedAssets);
      if (assetIds.isEmpty()) {
         return;
      }
      //filter sensors
      List<Asset> sensorFromPowerIQ = filterAssetsBySource(powerIQ.getId(), assetIds);
      realTimeDatas = getSensorRealTimeData(powerIQ, sensorFromPowerIQ);
      logger.info("Received new Sensor data, data item size is:" + realTimeDatas.size());
      if (realTimeDatas.isEmpty()) {
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
         Map<ServerSensorType, String> sensorsformularsmap = asset.getSensorsformulars();
         for (Map.Entry<ServerSensorType, String> map : sensorsformularsmap.entrySet()) {
            if (sensorType.contains(map.getKey())) {
               String[] assetIDs = map.getValue().split("\\+|-|\\*|/|\\(|\\)");
               for (String assetId : assetIDs) {
                  if (assetId.equals("")
                        || assetId.length() != FlowgateConstant.COUCHBASEIDLENGTH) {
                     continue;
                  }
                  assetIds.add(assetId);
               }
            }
         }
      }
      return assetIds;
   }

   public List<RealTimeData> getSensorRealTimeData(FacilitySoftwareConfig powerIQ,
         List<Asset> assets) {
      HashMap<AdvanceSettingType, String> advanceSetting = getAdvanceSetting(powerIQ);
      List<RealTimeData> realtimeDatas = new ArrayList<RealTimeData>();
      String dateFormat = advanceSetting.get(AdvanceSettingType.DateFormat);
      String timezone = advanceSetting.get(AdvanceSettingType.TimeZone);
      String temperature = advanceSetting.get(AdvanceSettingType.TEMPERATURE_UNIT);
      String humidity = advanceSetting.get(AdvanceSettingType.HUMIDITY_UNIT);
      PowerIQAPIClient powerIQAPIClient = createClient(powerIQ);
      for (Asset asset : assets) {
         HashMap<String, String> sensorExtraInfo = asset.getJustificationfields();
         String sensorInfo = sensorExtraInfo.get(FlowgateConstant.SENSOR);
         Map<String,String> sensorInfoMap = null;
         try {
            sensorInfoMap = getInfoMap(sensorInfo);
         } catch (IOException e2) {
           continue;
         }
         String sensorId = sensorInfoMap.get(FlowgateConstant.SENSOR_ID_FROM_POWERIQ);
         Sensor sensor = null;
         try {
            sensor = powerIQAPIClient.getSensorById(sensorId);
         } catch (HttpClientErrorException e) {
            logger.error("Failed to query data from PowerIQ", e);
            IntegrationStatus integrationStatus = powerIQ.getIntegrationStatus();
            if (integrationStatus == null) {
               integrationStatus = new IntegrationStatus();
            }
            integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
            integrationStatus.setDetail(e.getMessage());
            integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
            updateIntegrationStatus(powerIQ);
            break;
         } catch (ResourceAccessException e1) {
            if (e1.getCause().getCause() instanceof ConnectException) {
               checkAndUpdateIntegrationStatus(powerIQ, e1.getMessage());
               break;
            }
            break;
         }
         SensorReading reading = sensor.getReading();
         if (reading == null || reading.getId() == 0) {
            continue;
         }
         RealTimeData realTimeData = new RealTimeData();
         String valueDateTime = reading.getReadingTime();
         long recordedTime = WormholeDateFormat.getLongTime(valueDateTime, dateFormat, timezone);
         if (recordedTime == -1) {
            logger.error("Failed to translate the time string: " + valueDateTime
                  + ".And the dateformat is " + dateFormat);
            continue;
         }
         List<ValueUnit> values = new ArrayList<ValueUnit>();
         ValueUnit value = new ValueUnit();
         value.setTime(recordedTime);
         value.setKey(sensorValueType.get(sensor.getType()));

         String unit = reading.getUom();
         MetricUnit sourceUnit = null, targetUnit = null;
         switch (sensorValueType.get(sensor.getType())) {
         case HUMIDITY:
            if (unit != null && !unit.isEmpty()) {
               if (unit.equals("%")) {
                  sourceUnit = MetricUnit.PERCENT;
               } else {
                  sourceUnit = MetricUnit.valueOf(unit.toUpperCase());
               }
            } else {
               if (humidity.equals("%")) {
                  sourceUnit = MetricUnit.PERCENT;
               } else {
                  sourceUnit = MetricUnit.valueOf(humidity.toUpperCase());
               }

            }
            targetUnit = MetricUnit.PERCENT;
            break;
         case TEMP:
            if (unit != null && !unit.isEmpty()) {
               sourceUnit = MetricUnit.valueOf(unit.toUpperCase());
            } else {
               sourceUnit = MetricUnit.valueOf(temperature.toUpperCase());
            }
            targetUnit = MetricUnit.C;
            break;
         default:
            break;
         }

         try {
            value.setValueNum(Double.parseDouble(
                  value.translateUnit(String.valueOf(reading.getValue()), sourceUnit, targetUnit)));
         } catch (WormholeException e) {
            logger.error("Cannot translate Unit", e);
         }
         if (targetUnit.toString().equals(MetricUnit.PERCENT.toString())) {
            value.setUnit("%");
         } else {
            value.setUnit(targetUnit.toString());
         }

         values.add(value);
         realTimeData.setAssetID(asset.getId());
         realTimeData.setTime(recordedTime);
         realTimeData.setValues(values);
         realTimeData.setId(asset.getId() + "_" + recordedTime);
         realtimeDatas.add(realTimeData);
      }
      return realtimeDatas;
   }

   /**
    * This job will be removed after the job of aggregating PowerIQ and Nlyte done
    * @param powerIQ
    */
   public void syncPDUID(FacilitySoftwareConfig powerIQ) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      List<Asset> allPdusFromFlowgate = restClient.getAllAssetsByType(AssetCategory.PDU);
      //try to get all pdus from powerIQ;
      HashMap<String, Asset> pduAssetMapFromFlowgate = new HashMap<String, Asset>();
      for (Asset asset : allPdusFromFlowgate) {
         pduAssetMapFromFlowgate.put(asset.getAssetName().toLowerCase(), asset);
      }
      int limit = 100;
      int offset = 0;
      PowerIQAPIClient client = createClient(powerIQ);
      List<Pdu> pdus = null;
      while ((pdus = client.getPdus(limit, offset)) != null) {
         if (pdus.isEmpty()) {
            break;
         }
         List<Asset> needUpdateAssets = new ArrayList<Asset>();
         for (Pdu pdu : pdus) {
            if (pdu.getName() != null) {
               if (pduAssetMapFromFlowgate.containsKey(pdu.getName().toLowerCase())) {
                  Asset asset = pduAssetMapFromFlowgate.get(pdu.getName().toLowerCase());
                  String pduIDFromFlowgate = null;
                  String dataSource = null;
                  if (!asset.getJustificationfields().isEmpty()) {
                     pduIDFromFlowgate = asset.getJustificationfields().get(FlowgateConstant.PDU_ID_FROM_POWERIQ);
                     dataSource = asset.getJustificationfields().get(POWERIQ_SOURCE);
                  }
                  if (!String.valueOf(pdu.getId()).equals(pduIDFromFlowgate)
                        || dataSource == null) {
                     //we need to update the ID.
                     logger.info(String.format("Update Asset's PDU ID filed from %s to %s",
                           pduIDFromFlowgate, pdu.getId()));
                     asset.getJustificationfields().put(FlowgateConstant.PDU_ID_FROM_POWERIQ, String.valueOf(pdu.getId()));
                     asset.getJustificationfields().put(POWERIQ_SOURCE, powerIQ.getId());
                     needUpdateAssets.add(asset);
                  }

               } else {
                  //this PDU doesn't appeared in Nlyte.
                  logger.info(String.format("PDU with id %s from %s doesn't show up in Nlyte",
                        pdu.getId(), powerIQ.getServerURL()));
               }
            } else {
               logger.error(
                     String.format("The pdu with id %s don't have name field!", pdu.getId()));
            }
         }
         //update the asset
         if (!needUpdateAssets.isEmpty()) {
            restClient.saveAssets(needUpdateAssets);
         }
         offset += limit;
      }
   }

   public Map<String,Asset> getPDUIDAndAssetMap(List<Asset> pdusFromFlowgate){
      Map<String,Asset> pudIdAndAssetMap = new HashMap<String,Asset>();
      if(pdusFromFlowgate.isEmpty()) {
         return pudIdAndAssetMap;
      }
      for(Asset pdu : pdusFromFlowgate) {
         HashMap<String,String> justficationFields = pdu.getJustificationfields();
         String pduInfo = justficationFields.get(FlowgateConstant.PDU);
         if(pduInfo != null) {
            Map<String,String> pduInfoMap = null;
            try {
               pduInfoMap = getInfoMap(pduInfo);
            } catch (IOException e) {
               logger.error("Sync PDU from PowerIQ error",e.getCause());
               continue;
            }
            pudIdAndAssetMap.put(pduInfoMap.get(FlowgateConstant.PDU_ID_FROM_POWERIQ), pdu);
         }
      }
      return pudIdAndAssetMap;
   }

   private String generatePduOutletString(List<Outlet> pduOutlets) throws JsonProcessingException {
      List<PduOutlet> outletsSaveToFlowgate = new ArrayList<PduOutlet>();
      for(Outlet outlet : pduOutlets) {
         PduOutlet pduoutlet = new PduOutlet();
         pduoutlet.setDeviceId(outlet.getDeviceId());
         pduoutlet.setId(outlet.getId());
         pduoutlet.setName(outlet.getName());
         pduoutlet.setOrdinal(outlet.getOrdinal());
         pduoutlet.setPduId(outlet.getPduId());
         pduoutlet.setRatedAmps(outlet.getRatedAmps());
         pduoutlet.setState(outlet.getState());
         pduoutlet.setFormatedName(FlowgateConstant.OUTLET_NAME_PREFIX + outlet.getOrdinal());
         outletsSaveToFlowgate.add(pduoutlet);
      }
      return mapper.writeValueAsString(outletsSaveToFlowgate);
   }

   private String generatePduInletString(List<Inlet> pduInlets) throws JsonProcessingException {
      List<PduInlet> pduInletsSaveToFlowgate = new ArrayList<PduInlet>();
      for(Inlet inlet : pduInlets) {
         PduInlet pduInlet = new PduInlet();
         pduInlet.setId(inlet.getId());
         pduInlet.setOrdinal(inlet.getOrdinal());
         pduInlet.setPduId(inlet.getPduId());
         pduInlet.setPowerSource(inlet.isSource());
         pduInlet.setPueIt(inlet.isPueIt());
         pduInlet.setPueTotal(inlet.isPueTotal());
         pduInlet.setRatedAmps(inlet.getRatedAmps());
         pduInlet.setFormatedName(FlowgateConstant.INLET_NAME_PREFIX + inlet.getOrdinal());
         pduInletsSaveToFlowgate.add(pduInlet);
      }
      return mapper.writeValueAsString(pduInletsSaveToFlowgate);
   }
   public Map<String,String> generatePduRateInfoMap(Pdu pdu){

      Map<String,String> pduRateInfo = new HashMap<String,String>();
      String rateAmps = pdu.getRatedAmps();//eg: 32A
      String ratePower = pdu.getRatedVa();//eg: 6.4-7.7kVA
      String rateVolts = pdu.getRatedVolts();//eg: 200-240V
      String rateAmpsValue = null;
      if(rateAmps != null) {
         int ampsIndex = rateAmps.indexOf(POWERIQ_CURRENT_UNIT);
         if(ampsIndex != -1) {
            rateAmpsValue = rateAmps.substring(0, ampsIndex);
            pduRateInfo.put(FlowgateConstant.PDU_RATE_AMPS, rateAmpsValue);
         }else {
            logger.error(String.format("Invalid value for rate current : %s", rateAmps));
         }
      }
      if(ratePower != null) {
         int powerUnitIndex = ratePower.indexOf(POWERIQ_POWER_UNIT);
         if(powerUnitIndex != -1) {
            String powerValue = ratePower.substring(0, powerUnitIndex);
            int rangeFlagIndex = powerValue.indexOf(RANGE_FLAG);
            String minPowerValue = null;
            String maxPowerValue = null;
            if(rangeFlagIndex != -1) {
               minPowerValue = powerValue.substring(0, rangeFlagIndex);
               maxPowerValue = powerValue.substring(rangeFlagIndex + 1);
               pduRateInfo.put(FlowgateConstant.PDU_MIN_RATE_POWER, minPowerValue);
               pduRateInfo.put(FlowgateConstant.PDU_MAX_RATE_POWER, maxPowerValue);
            }else {
               //There is a another format of power value like 6.0kVA
               minPowerValue = powerValue;
               maxPowerValue = powerValue;
               pduRateInfo.put(FlowgateConstant.PDU_MIN_RATE_POWER, minPowerValue);
               pduRateInfo.put(FlowgateConstant.PDU_MAX_RATE_POWER, maxPowerValue);
            }
         }else {
            logger.error(String.format("Invalid value for rate power : %s",ratePower));
         }
      }
      if(rateVolts != null) {
         int voltageUnitIndex = rateVolts.indexOf(POWERIQ_VOLTAGE_UNIT);
         if(voltageUnitIndex != -1) {
            String voltageValue = rateVolts.substring(0, voltageUnitIndex);
            int rangeFlagIndex = voltageValue.indexOf(RANGE_FLAG);
            String minVoltageValue = null;
            String maxVoltageValue = null;
            if(rangeFlagIndex != -1) {
               minVoltageValue = voltageValue.substring(0, rangeFlagIndex);
               maxVoltageValue = voltageValue.substring(rangeFlagIndex + 1);
               pduRateInfo.put(FlowgateConstant.PDU_MIN_RATE_VOLTS, minVoltageValue);
               pduRateInfo.put(FlowgateConstant.PDU_MAX_RATE_VOLTS, maxVoltageValue);
            }else {
               //There is a another format of voltage value like 200V
               minVoltageValue = voltageValue;
               maxVoltageValue = voltageValue;
               pduRateInfo.put(FlowgateConstant.PDU_MIN_RATE_VOLTS, minVoltageValue);
               pduRateInfo.put(FlowgateConstant.PDU_MAX_RATE_VOLTS, maxVoltageValue);
            }
         }else {
            logger.error(String.format("Invalid value for rate voltage : %s",rateVolts));
         }
      }
      return pduRateInfo;
   }

   public void savePduAssetsToFlowgate(Map<String, Asset> existedPduAssets,
         String assetSource, PowerIQAPIClient client) {
      int limit = 100;
      int offset = 0;
      List<Pdu> pdus = null;
      List<Asset> assetsNeedToSave = null;
      while ((pdus = client.getPdus(limit, offset)) != null) {
         if (pdus.isEmpty()) {
            break;
         }
         assetsNeedToSave = new ArrayList<Asset>();
         for (Pdu pdu : pdus) {
            List<Outlet> outlets = client.getOutlets(pdu.getId());
            List<Inlet> inlets = client.getInlets(pdu.getId());
            Asset asset = fillLocation(pdu.getParent());
            String outletString = null;
            String inletString = null;
            try {
               outletString = generatePduOutletString(outlets);
               inletString = generatePduInletString(inlets);
            } catch (JsonProcessingException e) {
               logger.info(String.format("Sync pdu metadata error",e.getCause()));
            }
            Map<String,String> pduInfo = generatePduRateInfoMap(pdu);
            Asset existedPduAsset = existedPduAssets.get(String.valueOf(pdu.getId()));
            if(existedPduAsset != null) {
               existedPduAsset.setAssetName(pdu.getName());
               existedPduAsset.setLastupdate(System.currentTimeMillis());
               existedPduAsset.setSerialnumber(pdu.getSerialNumber());
               HashMap<String,String> oldJustficationfields = existedPduAsset.getJustificationfields();
               String oldPduInfo = oldJustficationfields.get(FlowgateConstant.PDU);
               Map<String,String> oldPduMap = null;
               try {
                  oldPduMap = getInfoMap(oldPduInfo);
               } catch (IOException e) {
                  logger.error("Sync pdu metadata error",e.getCause());
                  continue;
               }
               if(outletString != null) {
                  oldPduMap.put(FlowgateConstant.PDU_OUTLETS_FROM_POWERIQ, outletString);
               }
               if(inlets != null) {
                  oldPduMap.put(FlowgateConstant.PDU_INLETS_FROM_POWERIQ, inletString);
               }
               oldPduMap.put(FlowgateConstant.PDU_RATE_AMPS, pduInfo.get(FlowgateConstant.PDU_RATE_AMPS));
               oldPduMap.put(FlowgateConstant.PDU_MIN_RATE_POWER, pduInfo.get(FlowgateConstant.PDU_MIN_RATE_POWER));
               oldPduMap.put(FlowgateConstant.PDU_MAX_RATE_POWER, pduInfo.get(FlowgateConstant.PDU_MAX_RATE_POWER));
               oldPduMap.put(FlowgateConstant.PDU_MIN_RATE_VOLTS, pduInfo.get(FlowgateConstant.PDU_MIN_RATE_VOLTS));
               oldPduMap.put(FlowgateConstant.PDU_MAX_RATE_VOLTS, pduInfo.get(FlowgateConstant.PDU_MAX_RATE_VOLTS));
               try {
                  String newPduInfo = mapper.writeValueAsString(oldPduMap);
                  oldJustficationfields.put(FlowgateConstant.PDU, newPduInfo);
                  existedPduAsset.setJustificationfields(oldJustficationfields);
               } catch (JsonProcessingException e) {
                  logger.error("Sync pdu metadata error",e.getCause());
               }
               String assetSources[] = existedPduAsset.getAssetSource().split(FlowgateConstant.SPILIT_FLAG);
               if(assetSources.length == 1) { //So far this asset source is only have powerIQ
                  existedPduAsset.setRow(asset.getRow());
                  existedPduAsset.setRoom(asset.getRoom());
                  existedPduAsset.setFloor(asset.getFloor());
                  existedPduAsset.setCity(asset.getCity());
                  existedPduAsset.setCountry(asset.getCountry());
                  existedPduAsset.setExtraLocation(asset.getExtraLocation());
               }
               //save
               assetsNeedToSave.add(existedPduAsset);
            }else {
               asset.setAssetName(pdu.getName());
               asset.setSerialnumber(pdu.getSerialNumber());
               asset.setAssetSource(assetSource);
               asset.setCategory(AssetCategory.PDU);
               asset.setCreated(System.currentTimeMillis());
               if(outletString != null) {
                  pduInfo.put(FlowgateConstant.PDU_OUTLETS_FROM_POWERIQ, outletString);
               }
               if(inletString != null) {
                  pduInfo.put(FlowgateConstant.PDU_INLETS_FROM_POWERIQ, inletString);
               }
               pduInfo.put(FlowgateConstant.PDU_ID_FROM_POWERIQ, String.valueOf(pdu.getId()));
               String pduInfoString = null;
               try {
                  pduInfoString = mapper.writeValueAsString(pduInfo);
               } catch (JsonProcessingException e) {
                  logger.info(String.format("Sync pdu metadata error",e.getCause()));
               }
               if(pduInfoString != null) {
                  HashMap<String, String> justfacationfields = new HashMap<String, String>();
                  justfacationfields.put(FlowgateConstant.PDU, pduInfoString);
                  asset.setJustificationfields(justfacationfields);
               }
               //save
               assetsNeedToSave.add(asset);
            }
         }
         restClient.saveAssets(assetsNeedToSave);
         offset += limit;
      }
   }

   public Map<String,String> getInfoMap(String info) throws IOException{
      Map<String,String> infoMap = new HashMap<String,String>();
      if(info != null) {
         infoMap = mapper.readValue(info, new TypeReference<Map<String,String>>() {});
      }
      return infoMap;
   }

}
