/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.jobs;

import java.io.IOException;
import java.net.ConnectException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
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
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.MountingSide;
import com.vmware.flowgate.common.RealtimeDataUnit;
import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.PageModelImp;
import com.vmware.flowgate.common.model.PduInlet;
import com.vmware.flowgate.common.model.PduOutlet;
import com.vmware.flowgate.common.model.RealTimeData;
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
import com.vmware.flowgate.poweriqworker.client.PowerIQAPIClient;
import com.vmware.flowgate.poweriqworker.config.ServiceKeyConfig;
import com.vmware.flowgate.poweriqworker.model.Aisle;
import com.vmware.flowgate.poweriqworker.model.DataCenter;
import com.vmware.flowgate.poweriqworker.model.Floor;
import com.vmware.flowgate.poweriqworker.model.Inlet;
import com.vmware.flowgate.poweriqworker.model.InletReading;
import com.vmware.flowgate.poweriqworker.model.LocationInfo;
import com.vmware.flowgate.poweriqworker.model.Outlet;
import com.vmware.flowgate.poweriqworker.model.OutletReading;
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

   @Autowired
   private MessagePublisher publisher;

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
   private static Map<String, AssetSubCategory> subCategoryMap =
         new HashMap<String, AssetSubCategory>();
   public static Map<String, String> sensorAndMetricMap = new HashMap<String, String>();
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
      sensorAndMetricMap.put(HumiditySensor, MetricName.HUMIDITY);
      sensorAndMetricMap.put(TemperatureSensor, MetricName.TEMPERATURE);
      sensorAndMetricMap.put(AirFlowSensor, MetricName.AIR_FLOW);
      sensorAndMetricMap.put(AirPressureSensor, MetricName.AIRPRESSURE);
      sensorAndMetricMap.put(ContactClosureSensor, MetricName.CONTACTCLOSURE);
      sensorAndMetricMap.put(SmokeSensor, MetricName.SMOKE);
      sensorAndMetricMap.put(WaterSensor, MetricName.WATER_FLOW);
      sensorAndMetricMap.put(Vibration, MetricName.VIBRATION);
      sensorMountingSide.put("INLET", MountingSide.Front);
      sensorMountingSide.put("OUTLET", MountingSide.Back);
      sensorMountingSide.put("EXTERNAL", MountingSide.External);
      sensorMountingSide = Collections.unmodifiableMap(sensorMountingSide);
   }
   private static final String POWERIQ_VA_UNIT = "kVA";
   private static final String POWERIQ_POWER_UNIT = "W";
   private static final String POWERIQ_CURRENT_UNIT = "A";
   private static final String POWERIQ_VOLTAGE_UNIT = "V";
   private static final String RANGE_FLAG = "-";

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
         logger.info("Finish sync realtime data for " + powerIQ.getName());
         break;
      case EventMessageUtil.PowerIQ_CleanInActiveAssetData:
         logger.info("Clean sensor metadata for " + powerIQ.getName());
         removeSensorMetaData(powerIQ);
         logger.info("Finish clean sensor metadata for " + powerIQ.getName());
         break;
      case EventMessageUtil.PowerIQ_SyncAllSensorMetricFormula:
         syncAllSensorMetricFormula();
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

   public void syncAllSensorMetricFormula() {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      FacilitySoftwareConfig[] powerIQs =
            restClient.getFacilitySoftwareByType(SoftwareType.PowerIQ).getBody();
      for(FacilitySoftwareConfig powerIQ : powerIQs) {
         logger.info("Start sync sensor metrics formula for " + powerIQ.getName());
         List<Asset> pdusFromFlowgate = restClient.getAllAssetsBySourceAndType(powerIQ.getId(), AssetCategory.PDU);
         Map<String, Asset> pduIDAndAssetMap = getPDUIDAndAssetMap(pdusFromFlowgate);
         int pageSize = 200;
         int pageNumber = 0;
         List<Asset> sensors = null;
         ResponseEntity<PageModelImp<Asset>> res =
               restClient.getAssetsBySourceAndType(powerIQ.getId(), AssetCategory.Sensors,pageNumber,pageSize);
         while (!res.getBody().getContent().isEmpty()) {
            sensors = res.getBody().getContent();
            Set<Asset> pduAssetNeedToUpdate = updatePduMetricformular(sensors,pduIDAndAssetMap);
            restClient.saveAssets(new ArrayList<Asset>(pduAssetNeedToUpdate));
            pageNumber++;
            res =
                  restClient.getAssetsBySourceAndType(powerIQ.getId(), AssetCategory.Sensors,pageNumber,pageSize);
         }
         logger.info("Finished sync sensor metrics formula for " + powerIQ.getName());
      }
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
         Map<String, Map<String, Map<String, String>>> formulars = server.getMetricsformulars();
         if(formulars == null || formulars.isEmpty()) {
            continue;
         }
         Map<String, Map<String, String>> sensorFormulars = formulars.get(FlowgateConstant.SENSOR);
         for(Map.Entry<String, Map<String, String>> sensorFormularMap : sensorFormulars.entrySet()) {
            Map<String, String> sensorLocationAndIdMap = sensorFormularMap.getValue();
            Iterator<Map.Entry<String, String>> ite = sensorLocationAndIdMap.entrySet().iterator();
            while(ite.hasNext()) {
               Map.Entry<String, String> map = ite.next();
               String sensorIdFromServer = map.getValue();
               if(sensorIdFromServer.indexOf(sensorId) != -1) {
                  changed = true;
                  ite.remove();
               }
            }
         }
         server.setMetricsformulars(formulars);
         if(changed) {
            needToUpdate.add(server);
         }
      }
      return needToUpdate;
   }

   public LocationInfo getLocationInfo(PowerIQAPIClient client) {
      LocationInfo location = new LocationInfo();
      location.setAislesMap(getAislesMap(client));
      location.setRacksMap(getRacksMap(client));
      location.setRowsMap(getRowsMap(client));
      location.setRoomsMap(getRoomsMap(client));
      location.setFloorsMap( getFloorsMap(client));
      location.setDataCentersMap(getDataCentersMap(client));
      return location;
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
      LocationInfo location = getLocationInfo(client);
      List<Asset> pdusFromFlowgate = restClient.getAllAssetsBySourceAndType(powerIQ.getId(), AssetCategory.PDU);
      logger.info("Size :" + pdusFromFlowgate.size());
      Map<String, Asset> pduIDAndAssetMap = getPDUIDAndAssetMap(pdusFromFlowgate);
      logger.info("Map Size :" + pduIDAndAssetMap.size());

      savePduAssetsToFlowgate(pduIDAndAssetMap, powerIQ.getId(), client, location);
      logger.info("Finish sync PDU metadata for " + powerIQ.getName());

      Map<String, Asset> exsitingSensorAssets = getAssetsFromWormhole(powerIQ.getId());
      saveSensorAssetsToFlowgate(exsitingSensorAssets, client, powerIQ.getId(), pduIDAndAssetMap, location);
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
         PowerIQAPIClient client, String assetSource, Map<String, Asset> pduAssetMap, LocationInfo location) {
      List<Sensor> sensors = null;
      int limit = 100;
      int offset = 0;
      List<Asset> newAssetsNeedToSave = null;
      List<Asset> oldAssetsNeedToupdate = null;
      while ((sensors = client.getSensors(limit, offset)) != null) {
         if (sensors.isEmpty()) {
            logger.warn(
                  String.format("No sensor data from /api/v2/sensors?limit=%s&offset=%s", limit, offset ));
            break;
         }
         newAssetsNeedToSave = new ArrayList<Asset>();
         oldAssetsNeedToupdate = new ArrayList<Asset>();
         for (Sensor sensor : sensors) {
            //Filter AbsoluteHumiditySensor
            if(subCategoryMap.get(sensor.getType()) == null) {
               continue;
            }
            Asset asset = new Asset();
            Map<String,String> sensorMap = new HashMap<String,String>();
            sensorMap.put(FlowgateConstant.SENSOR_ID_FROM_POWERIQ, String.valueOf(sensor.getId()));
            sensorMap.put(FlowgateConstant.POSITION, sensor.getPosition());
            if (sensor.getPduId() != null) {
               Asset pduAsset = pduAssetMap.get(String.valueOf(sensor.getPduId()));
               if (pduAsset == null) {
                  asset = fillLocation(sensor.getParent(), location);
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
               }
            } else {
               asset = fillLocation(sensor.getParent(), location);
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
                  oldSensorInfoMap.put(FlowgateConstant.POSITION, sensorMap.get(FlowgateConstant.POSITION));
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
               oldAssetsNeedToupdate.add(assetToUpdate);
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
               newAssetsNeedToSave.add(asset);
            }
         }
         restClient.saveAssets(oldAssetsNeedToupdate);
         if(!newAssetsNeedToSave.isEmpty()) {
            //We need the assetId of sensor asset, so it should be saved first.
            List<Asset> sensorAlreadySaved = new ArrayList<Asset>();
            for(Asset asset : newAssetsNeedToSave) {
               ResponseEntity<Void> res = restClient.saveAssets(asset);
               if(res.getStatusCode().is2xxSuccessful()) {
                  String uriPath = res.getHeaders().getLocation().getPath();
                  String id = uriPath.substring(uriPath.lastIndexOf("/") + 1);
                  asset.setId(id);
                  sensorAlreadySaved.add(asset);
               }
            }
            Set<Asset> pduAssetNeedToUpdate = updatePduMetricformular(sensorAlreadySaved,pduAssetMap);
            restClient.saveAssets(new ArrayList<Asset>(pduAssetNeedToUpdate));
         }
         offset += limit;
      }
   }

   public Set<Asset> updatePduMetricformular(List<Asset> sensorAssets, Map<String,Asset> pduIdAndAssetMap){
      //Different sensors may have the same pduId.
      Set<Asset> pduAssets = new HashSet<Asset>();
      for(Asset sensorAsset : sensorAssets) {
         com.vmware.flowgate.common.model.Parent parent = sensorAsset.getParent();
         if(parent == null) {
            continue;
         }
         String pduId = parent.getParentId();
         Asset pduAsset = pduIdAndAssetMap.get(pduId);
         if(pduAsset == null) {
            continue;
         }
         int rackUnitNumber = sensorAsset.getCabinetUnitPosition();
         String positionInfo = FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION;
         Map<String,String> sensorAssetJustfication = sensorAsset.getJustificationfields();
         String rackUnitInfo = null;
         String positionFromAsset = null;

         if(rackUnitNumber != 0) {
            rackUnitInfo = FlowgateConstant.RACK_UNIT_PREFIX + rackUnitNumber;
         }
         if(sensorAssetJustfication != null) {
            String sensorInfo = sensorAssetJustfication.get(FlowgateConstant.SENSOR);
            if(sensorInfo != null) {
               try {
                  Map<String,String> sensorInfoMap = getInfoMap(sensorInfo);
                  positionFromAsset = sensorInfoMap.get(FlowgateConstant.POSITION);
               } catch (IOException e) {
                  positionFromAsset = null;
               }
            }
         }
         //formats of position info : 1.rackUnit1 2.rackUnit1_FIELDSPLIT_INLET 3.INLET/OUTLET/EXTRNAL/COMMON
         if(rackUnitInfo != null) {
            positionInfo = rackUnitInfo;
         }
         if(positionFromAsset != null) {
            if(positionInfo != null) {
               positionInfo = positionInfo + FlowgateConstant.SEPARATOR + positionFromAsset;
            }else {
               positionInfo = positionFromAsset;
            }
         }
         Map<String, Map<String, Map<String, String>>> formulars = pduAsset.getMetricsformulars();
         if(formulars == null || formulars.isEmpty()) {
            formulars = new HashMap<String, Map<String, Map<String, String>>>();
            Map<String, Map<String, String>> sensorFormulars = generateNewMetricformular(sensorAsset, positionInfo);
            if(sensorFormulars.isEmpty()) {
               continue;
            }
            formulars.put(FlowgateConstant.SENSOR, sensorFormulars);
         }else {
            Map<String, Map<String, String>> sensorFormulars = formulars.get(FlowgateConstant.SENSOR);
            if(sensorFormulars == null || sensorFormulars.isEmpty()) {
               sensorFormulars = generateNewMetricformular(sensorAsset, positionInfo);
               if(sensorFormulars.isEmpty()) {
                  continue;
               }
               formulars.put(FlowgateConstant.SENSOR, sensorFormulars);
            }else {
               Map<String,String> locationAndMetricMap = null;
               switch (sensorAsset.getSubCategory()) {
               case Humidity:
                  locationAndMetricMap = sensorFormulars.get(MetricName.PDU_HUMIDITY);
                  if(locationAndMetricMap == null) {
                     locationAndMetricMap = new HashMap<String,String>();
                  }
                  locationAndMetricMap.put(positionInfo, sensorAsset.getId());
                  sensorFormulars.put(MetricName.PDU_HUMIDITY, locationAndMetricMap);
                  break;
               case Temperature:
                  locationAndMetricMap = sensorFormulars.get(MetricName.PDU_TEMPERATURE);
                  if(locationAndMetricMap == null) {
                     locationAndMetricMap = new HashMap<String,String>();
                  }
                  locationAndMetricMap.put(positionInfo, sensorAsset.getId());
                  sensorFormulars.put(MetricName.PDU_TEMPERATURE, locationAndMetricMap);
                  break;
               default:
                  break;
               }
            }
            formulars.put(FlowgateConstant.SENSOR, sensorFormulars);
         }
         pduAsset.setMetricsformulars(formulars);
         pduAssets.add(pduAsset);
      }
      return pduAssets;
   }

   public Map<String,Map<String,String>> generateNewMetricformular(Asset sensorAsset, String positionInfo){
      Map<String, Map<String, String>> sensorFormulars = new HashMap<String, Map<String, String>>();
      Map<String, String> metricsLocationAndIdMap = new HashMap<String,String>();
      switch (sensorAsset.getSubCategory()) {
      case Humidity:
         metricsLocationAndIdMap.put(positionInfo, sensorAsset.getId());
         sensorFormulars.put(MetricName.PDU_HUMIDITY, metricsLocationAndIdMap);
         break;
      case Temperature:
         metricsLocationAndIdMap.put(positionInfo, sensorAsset.getId());
         sensorFormulars.put(MetricName.PDU_TEMPERATURE, metricsLocationAndIdMap);
         break;
      default:
         break;
      }
      return sensorFormulars;
   }

   /**
    * Record the sensorId and sensor source for the pdu.
    * @param pduAsset
    * @param sensor
    * @param sensorSource
    * @return
    */
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

   public Asset fillLocation(Parent parent, LocationInfo location) {
      Asset asset = null;
      StringBuilder extraLocation = new StringBuilder();
      asset = new Asset();
      if (parent == null) {
         return asset;
      }
      Map<Long, Rack> racksMap = location.getRacksMap();
      Map<Long, Row> rowsMap = location.getRowsMap();
      Map<Long, Aisle> aislesMap = location.getAislesMap();
      Map<Long, Room> roomsMap = location.getRoomsMap();
      Map<Long, Floor> floorsMap = location.getFloorsMap();
      Map<Long, DataCenter> dataCentersMap = location.getDataCentersMap();

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
            if(rack == null) {
               logger.info("Invalid rack id : " + parent.getId());
               parent = null;
               break;
            }
            extraLocation.append(rack_type + ":" + rack.getName() + "" + ";");
            parent = rack.getParent();
            break;
         case row_type:
            if (rowMapisEmpty) {
               parent = null;
               break;
            }
            Row row = rowsMap.get(parent.getId());
            if(row == null) {
               logger.info("Invalid row id : " + parent.getId());
               parent = null;
               break;
            }
            asset.setRow(row.getName());
            parent = row.getParent();
            break;
         case aisle_type:
            if (aislesMapIsEmpty) {
               parent = null;
               break;
            }
            Aisle ailse = aislesMap.get(parent.getId());
            if(ailse == null) {
               logger.info("Invalid ailse id : " + parent.getId());
               parent = null;
               break;
            }
            extraLocation.append(aisle_type + ":" + ailse.getName() + "");
            parent = ailse.getParent();
            break;
         case room_type:
            if (roomsMapIsEmpty) {
               parent = null;
               break;
            }
            Room room = roomsMap.get(parent.getId());
            if(room == null) {
               logger.info("Invalid room id : " + parent.getId());
               parent = null;
               break;
            }
            asset.setRoom(room.getName());
            parent = room.getParent();
            break;
         case floor_type:
            if (floorMapIsEmpty) {
               parent = null;
               break;
            }
            Floor floor = floorsMap.get(parent.getId());
            if(floor == null) {
               logger.info("Invalid floor id : " + parent.getId());
               parent = null;
               break;
            }
            asset.setFloor(floor.getName());
            parent = floor.getParent();
            break;
         case dataCenter_type:
            if (dataCenterMapIsEmpty) {
               parent = null;
               break;
            }
            DataCenter dataCenter = dataCentersMap.get(parent.getId());
            if(dataCenter == null) {
               logger.info("Invalid dataCenter id : " + parent.getId());
               parent = null;
               break;
            }
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
      Map<String, Map<String,String>> pduAssetIdAndPduInfoMap = new HashMap<String, Map<String,String>>();
      for (Asset asset : allMappedPdus) {
         String id = null;
         Map<String,String> pduInfoMap = null;
         if (asset.getJustificationfields() != null) {
            HashMap<String,String> justficationfields = asset.getJustificationfields();
            String pduInfo = justficationfields.get(FlowgateConstant.PDU);
            try {
              pduInfoMap = getInfoMap(pduInfo);
            } catch (IOException e) {
               continue;
            }
            id = pduInfoMap.get(FlowgateConstant.PDU_ID_FROM_POWERIQ);
            //Only check the pdu that belong to the current PowerIQ.
            if (!asset.getAssetSource().contains((powerIQ.getId()))) {
               continue;
            }
         }
         if (id != null) {
            pduAssetIdAndPduInfoMap.put(asset.getId(), pduInfoMap);
         }

      }
      List<RealTimeData> pduMetricRealTimeDatas = getRealTimeDatas(pduAssetIdAndPduInfoMap,client, getAdvanceSetting(powerIQ));
      if (!pduMetricRealTimeDatas.isEmpty()) {
         restClient.saveRealTimeData(pduMetricRealTimeDatas);
         logger.info("Finish sync pdu realtime data for " + powerIQ.getName());
      }
      logger.info("Not found any pdu realtime data from " + powerIQ.getName());

      List<Asset> allMappedServers =
            Arrays.asList(restClient.getMappedAsset(AssetCategory.Server).getBody());
      if (allMappedServers.isEmpty()) {
         logger.info("No mapped server found. End sync RealTime data Job");
         return;
      }
      //get sensor assetIds from pdu's metricsFormular attribute
      Set<String> sensorAssetIds = getAssetIdfromformular(allMappedPdus);
      //get sensor assetIds from server's metricsFromular attribute
      Set<String> sensorAssetIdsFromServer = getAssetIdfromformular(allMappedServers);
      sensorAssetIds.addAll(sensorAssetIdsFromServer);
      if (sensorAssetIds.isEmpty()) {
         return;
      }
      //filter sensors
      List<Asset> sensorFromPowerIQ = filterAssetsBySource(powerIQ.getId(), sensorAssetIds);
      List<RealTimeData> realTimeDatas = getSensorRealTimeData(powerIQ, sensorFromPowerIQ);
      logger.info("Received new Sensor data, data item size is:" + realTimeDatas.size());
      if (realTimeDatas.isEmpty()) {
         return;
      }
      restClient.saveRealTimeData(realTimeDatas);
      logger.info("Finish sync sensor realtime data for " + powerIQ.getName());
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

   public List<RealTimeData> getRealTimeDatas(Map<String, Map<String,String>> pduAssetIdAndPduInfoMap, PowerIQAPIClient client,
         HashMap<AdvanceSettingType, String> advanceSettingMap) {
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      if (pduAssetIdAndPduInfoMap == null || pduAssetIdAndPduInfoMap.isEmpty()) {
         return realTimeDatas;
      }
      try {
         for (Map.Entry<String, Map<String,String>> map : pduAssetIdAndPduInfoMap.entrySet()) {
            List<ValueUnit> values = getValueUnits(map.getValue(), client, advanceSettingMap);
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

   public List<ValueUnit> getValueUnits(Map<String,String> pduInfoFromPowerIQ, PowerIQAPIClient client,
         HashMap<AdvanceSettingType, String> advanceSettingMap) {
      List<ValueUnit> values = new ArrayList<ValueUnit>();
      Long pduId = Long.parseLong(pduInfoFromPowerIQ.get(FlowgateConstant.PDU_ID_FROM_POWERIQ));
      List<Outlet> outlets = client.getOutlets(pduId);
      List<Inlet> inlets = client.getInlets(pduId);
      if (outlets.isEmpty() && inlets.isEmpty()) {
         return values;
      }
      String dateFormat = advanceSettingMap.get(AdvanceSettingType.DateFormat);
      String timezone = advanceSettingMap.get(AdvanceSettingType.TimeZone);
      String PDU_AMPS_UNIT = advanceSettingMap.get(AdvanceSettingType.PDU_AMPS_UNIT);
      String PDU_POWER_UNIT = advanceSettingMap.get(AdvanceSettingType.PDU_POWER_UNIT);
      String PDU_VOLT_UNIT = advanceSettingMap.get(AdvanceSettingType.PDU_VOLT_UNIT);

      if(PDU_AMPS_UNIT == null) {
       //By default the Current unit in powerIQ is Amps.
         PDU_AMPS_UNIT = POWERIQ_CURRENT_UNIT;
      }
      if(PDU_POWER_UNIT == null) {
         //By default the active_power unit in powerIQ is watt.
         PDU_POWER_UNIT = POWERIQ_POWER_UNIT;
      }
      if(PDU_VOLT_UNIT == null) {
         //By default the voltage unit in powerIQ is volt.
         PDU_VOLT_UNIT = POWERIQ_VOLTAGE_UNIT;
      }
      if(!inlets.isEmpty()) {
         String time = inlets.get(0).getReading().getReadingTime();
         long valueTime = WormholeDateFormat.getLongTime(time, dateFormat, timezone);
         if (valueTime == -1) {
            logger.error("Failed to translate the time string: " + time + ".And the dateformat is "
                  + dateFormat);
         }else {
            double totalPower = 0.0;
            double totalCurrent = 0.0;
            for(Inlet inlet : inlets) {
               InletReading reading = inlet.getReading();
               String extraIdentifier = FlowgateConstant.INLET_NAME_PREFIX + inlet.getOrdinal();

               Double voltage = reading.getVoltage();
               if(voltage != null) {
                  ValueUnit voltageValue = new ValueUnit();
                  voltageValue.setExtraidentifier(extraIdentifier);
                  voltageValue.setKey(MetricName.PDU_VOLTAGE);
                  voltageValue.setTime(valueTime);
                  voltageValue
                        .setValueNum(Double.parseDouble(voltageValue.translateUnit(String.valueOf(voltage),
                              MetricUnit.valueOf(PDU_VOLT_UNIT), MetricUnit.V)));
                  voltageValue.setUnit(RealtimeDataUnit.Volts.toString());
                  values.add(voltageValue);
               }

               Double current = reading.getCurrent();
               if(current != null) {
                  ValueUnit currentValue = new ValueUnit();
                  currentValue.setExtraidentifier(extraIdentifier);
                  currentValue.setKey(MetricName.PDU_CURRENT);
                  currentValue.setTime(valueTime);
                  currentValue
                        .setValueNum(Double.parseDouble(currentValue.translateUnit(String.valueOf(current),
                              MetricUnit.valueOf(PDU_AMPS_UNIT), MetricUnit.A)));
                  currentValue.setUnit(RealtimeDataUnit.Amps.toString());
                  values.add(currentValue);
                  totalCurrent += currentValue.getValueNum();
               }

               Double activePower = reading.getActivePower();
               if(activePower != null) {
                  ValueUnit activePowerValue = new ValueUnit();
                  activePowerValue.setExtraidentifier(extraIdentifier);
                  activePowerValue.setKey(MetricName.PDU_ACTIVE_POWER);
                  activePowerValue.setTime(valueTime);
                  activePowerValue.setValueNum(Double.parseDouble(activePowerValue.translateUnit(String.valueOf(activePower),
                        MetricUnit.valueOf(PDU_POWER_UNIT), MetricUnit.KW)));
                  activePowerValue.setUnit(RealtimeDataUnit.KW.toString());
                  values.add(activePowerValue);
               }

               Double apparentPower = reading.getApparentPower();
               if(apparentPower != null) {
                  ValueUnit apparentPowerValue = new ValueUnit();
                  apparentPowerValue.setExtraidentifier(extraIdentifier);
                  apparentPowerValue.setKey(MetricName.PDU_APPARENT_POWER);
                  apparentPowerValue.setTime(valueTime);
                  apparentPowerValue.setValueNum(Double.parseDouble(apparentPowerValue.translateUnit(String.valueOf(apparentPower),
                        MetricUnit.valueOf(PDU_POWER_UNIT), MetricUnit.KW)));
                  apparentPowerValue.setUnit(RealtimeDataUnit.KW.toString());
                  values.add(apparentPowerValue);
                  totalPower += apparentPowerValue.getValueNum();
               }

               Double freeCapacity = reading.getUnutilizedCapacity();
               if(freeCapacity != null) {
                  ValueUnit freeCapacityValue = new ValueUnit();
                  freeCapacityValue.setExtraidentifier(extraIdentifier);
                  freeCapacityValue.setKey(MetricName.PDU_FREE_CAPACITY);
                  freeCapacityValue.setTime(valueTime);
                  freeCapacityValue
                        .setValueNum(Double.parseDouble(freeCapacityValue.translateUnit(String.valueOf(freeCapacity),
                              MetricUnit.valueOf(PDU_AMPS_UNIT), MetricUnit.A)));
                  freeCapacityValue.setUnit(RealtimeDataUnit.Amps.toString());
                  values.add(freeCapacityValue);
               }
            }
            ValueUnit total_current = new ValueUnit();
            total_current.setKey(MetricName.PDU_TOTAL_CURRENT);
            total_current.setTime(valueTime);
            total_current.setUnit(RealtimeDataUnit.Amps.toString());
            total_current.setValueNum(totalCurrent);
            values.add(total_current);

            ValueUnit total_power = new ValueUnit();
            total_power.setKey(MetricName.PDU_TOTAL_POWER);
            total_power.setTime(valueTime);
            total_power.setUnit(RealtimeDataUnit.KW.toString());
            total_power.setValueNum(totalPower);
            values.add(total_power);
         }
      }

      if(!outlets.isEmpty()) {
         String time = inlets.get(0).getReading().getReadingTime();
         long valueTime = WormholeDateFormat.getLongTime(time, dateFormat, timezone);
         if (valueTime == -1) {
            logger.error("Failed to translate the time string: " + time + ".And the dateformat is "
                  + dateFormat);
         }else {
            double totalCurrentUsed = 0.0;
            double totalPowerUsed = 0.0;
            for(Outlet outlet : outlets) {
               OutletReading reading = outlet.getReading();
               String extraIdentifier = FlowgateConstant.OUTLET_NAME_PREFIX + outlet.getOrdinal();
               Double voltage = reading.getVoltage();
               if(voltage != null) {
                  ValueUnit voltageValue = new ValueUnit();
                  voltageValue.setExtraidentifier(extraIdentifier);
                  voltageValue.setKey(MetricName.PDU_VOLTAGE);
                  voltageValue.setTime(valueTime);
                  voltageValue
                        .setValueNum(Double.parseDouble(voltageValue.translateUnit(String.valueOf(voltage),
                              MetricUnit.valueOf(PDU_VOLT_UNIT), MetricUnit.V)));
                  voltageValue.setUnit(RealtimeDataUnit.Volts.toString());
                  values.add(voltageValue);
               }

               Double current = reading.getCurrent();
               if(current != null) {
                  ValueUnit currentValue = new ValueUnit();
                  currentValue.setExtraidentifier(extraIdentifier);
                  currentValue.setKey(MetricName.PDU_CURRENT);
                  currentValue.setTime(valueTime);
                  currentValue
                        .setValueNum(Double.parseDouble(currentValue.translateUnit(String.valueOf(current),
                              MetricUnit.valueOf(PDU_AMPS_UNIT), MetricUnit.A)));
                  currentValue.setUnit(RealtimeDataUnit.Amps.toString());
                  values.add(currentValue);
                  totalCurrentUsed += currentValue.getValueNum();
               }

               Double active_power = reading.getActivePower();
               if(active_power != null) {
                  ValueUnit activePowerValue = new ValueUnit();
                  activePowerValue.setExtraidentifier(extraIdentifier);
                  activePowerValue.setKey(MetricName.PDU_ACTIVE_POWER);
                  activePowerValue.setTime(valueTime);
                  activePowerValue.setValueNum(Double.parseDouble(activePowerValue.translateUnit(String.valueOf(active_power),
                        MetricUnit.valueOf(PDU_POWER_UNIT), MetricUnit.KW)));
                  activePowerValue.setUnit(RealtimeDataUnit.KW.toString());
                  values.add(activePowerValue);
               }

               Double apparent_power = reading.getApparentPower();
               if(apparent_power != null) {
                  ValueUnit apparentPowerValue = new ValueUnit();
                  apparentPowerValue.setExtraidentifier(extraIdentifier);
                  apparentPowerValue.setKey(MetricName.PDU_APPARENT_POWER);
                  apparentPowerValue.setTime(valueTime);
                  apparentPowerValue.setValueNum(Double.parseDouble(apparentPowerValue.translateUnit(String.valueOf(apparent_power),
                        MetricUnit.valueOf(PDU_POWER_UNIT), MetricUnit.KW)));
                  apparentPowerValue.setUnit(RealtimeDataUnit.KW.toString());
                  values.add(apparentPowerValue);
                  totalPowerUsed += apparentPowerValue.getValueNum();
               }

               Double freeCapacity = reading.getUnutilizedCapacity();
               if(freeCapacity != null) {
                  ValueUnit freeCapacityValue = new ValueUnit();
                  freeCapacityValue.setExtraidentifier(extraIdentifier);
                  freeCapacityValue.setKey(MetricName.PDU_FREE_CAPACITY);
                  freeCapacityValue.setTime(valueTime);
                  freeCapacityValue
                        .setValueNum(Double.parseDouble(freeCapacityValue.translateUnit(String.valueOf(freeCapacity),
                              MetricUnit.valueOf(PDU_AMPS_UNIT), MetricUnit.A)));
                  freeCapacityValue.setUnit(RealtimeDataUnit.Amps.toString());
                  values.add(freeCapacityValue);
               }

            }
            String rate_current = pduInfoFromPowerIQ.get(FlowgateConstant.PDU_RATE_AMPS);
            DecimalFormat df = new DecimalFormat("#.00");
            if(rate_current != null) {
               Double rate_current_value = Double.parseDouble(rate_current);
               ValueUnit current_load = new ValueUnit();
               current_load.setKey(MetricName.PDU_CURRENT_LOAD);
               current_load.setTime(valueTime);
               current_load.setUnit(RealtimeDataUnit.Percent.toString());
               current_load.setValueNum(Double.parseDouble(df.format(totalCurrentUsed/rate_current_value)));
               values.add(current_load);
            }
            String rate_power = pduInfoFromPowerIQ.get(FlowgateConstant.PDU_MIN_RATE_POWER);
            if(rate_power != null) {
               Double rate_power_value = Double.parseDouble(rate_power);
               ValueUnit power_load = new ValueUnit();
               power_load.setKey(MetricName.PDU_POWER_LOAD);
               power_load.setTime(valueTime);
               power_load.setUnit(RealtimeDataUnit.Percent.toString());
               power_load.setValueNum(Double.parseDouble(df.format(totalPowerUsed/rate_power_value)));
               values.add(power_load);
            }
         }
      }
      return values;
   }

   public List<Asset> filterAssetsBySource(String source, Set<String> assetIds) {
      List<Asset> sensorAssets = new ArrayList<Asset>();
      List<Asset> assetsFromPowerIQ = restClient.getAllAssetsBySource(source);
      for (Asset asset : assetsFromPowerIQ) {
         if (assetIds.contains(asset.getId())) {
            sensorAssets.add(asset);
         }
      }
      return sensorAssets;
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

   public Set<String> getAssetIdfromformular(List<Asset> mappedAssets) {
      Set<String> assetIds = new HashSet<String>();
      for (Asset asset : mappedAssets) {
         Map<String, Map<String, Map<String, String>>> formulars = asset.getMetricsformulars();
         if(formulars == null || formulars.isEmpty()) {
            continue;
         }
         Map<String, Map<String, String>> sensorFormulars = formulars.get(FlowgateConstant.SENSOR);
         if(sensorFormulars == null || sensorFormulars.isEmpty()) {
            continue;
         }
         for(Map.Entry<String, Map<String, String>> sensorFormularMap : sensorFormulars.entrySet()) {
            Map<String, String> sensorLocationAndIdMap = sensorFormularMap.getValue();
            for(String formular : sensorLocationAndIdMap.values()) {
               assetIds.addAll(Arrays.asList(formular.split("\\+|-|\\*|/|\\(|\\)")));
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
         value.setKey(sensorAndMetricMap.get(sensor.getType()));

         String unit = reading.getUom();
         MetricUnit sourceUnit = null, targetUnit = null;
         switch (sensorAndMetricMap.get(sensor.getType())) {
         case MetricName.HUMIDITY:
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
         case MetricName.TEMPERATURE:
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

         Double metricsValue = reading.getValue();
         if(metricsValue == null) {
            continue;
         }
         try {
            value.setValueNum(Double.parseDouble(
                  value.translateUnit(String.valueOf(metricsValue), sourceUnit, targetUnit)));
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
            if(NumberUtils.isCreatable(rateAmps)) {
               //another format : only include number eg:32
               pduRateInfo.put(FlowgateConstant.PDU_RATE_AMPS, rateAmps);
            }else {
               logger.error(String.format("Invalid value for rate current : %s", rateAmps));
            }
         }
      }
      if(ratePower != null) {
         int powerUnitIndex = ratePower.indexOf(POWERIQ_VA_UNIT);
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
            if(NumberUtils.isCreatable(ratePower)) {
               //another format : only include number eg:6.0
               pduRateInfo.put(FlowgateConstant.PDU_MIN_RATE_POWER, ratePower);
               pduRateInfo.put(FlowgateConstant.PDU_MAX_RATE_POWER, ratePower);
            }else {
               logger.error(String.format("Invalid value for rate power : %s",ratePower));
            }
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
            if(NumberUtils.isCreatable(rateVolts)) {
               //another format : only include number eg:220
               pduRateInfo.put(FlowgateConstant.PDU_MIN_RATE_VOLTS, rateVolts);
               pduRateInfo.put(FlowgateConstant.PDU_MAX_RATE_VOLTS, rateVolts);
            }else {
               logger.error(String.format("Invalid value for rate voltage : %s",rateVolts));
            }
         }
      }
      return pduRateInfo;
   }

   public void savePduAssetsToFlowgate(Map<String, Asset> existedPduAssets,
         String assetSource, PowerIQAPIClient client, LocationInfo location) {
      int limit = 100;
      int offset = 0;
      List<Pdu> pdus = null;
      List<Asset> assetsNeedToSave = null;
      boolean triggerPDUAggregation = false;
      while ((pdus = client.getPdus(limit, offset)) != null) {
         if (pdus.isEmpty()) {
            break;
         }
         assetsNeedToSave = new ArrayList<Asset>();
         for (Pdu pdu : pdus) {
            List<Outlet> outlets = client.getOutlets(pdu.getId());
            List<Inlet> inlets = client.getInlets(pdu.getId());
            Asset asset = fillLocation(pdu.getParent(), location);
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
               triggerPDUAggregation = true;
            }
         }
         restClient.saveAssets(assetsNeedToSave);
         offset += limit;
      }
      if(triggerPDUAggregation) {
         try {
            EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.Aggregator,
                  EventMessageUtil.AggregateAndCleanPowerIQPDU, "");
            String jobmessage = EventMessageUtil.convertEventMessageAsString(eventMessage);
            publisher.publish(EventMessageUtil.AggregatorTopic, jobmessage);
            logger.info("Send aggregate Pdu data command");
         }catch(IOException e) {
            logger.error("Failed to Send aggregate pdu data command", e);
         }
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
