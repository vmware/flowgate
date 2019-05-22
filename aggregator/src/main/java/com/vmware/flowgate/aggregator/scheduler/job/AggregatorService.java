/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.aggregator.scheduler.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.ServerSensorData.ServerSensorType;
import com.vmware.flowgate.common.model.redis.message.AsyncService;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.EventUser;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;

@Service
public class AggregatorService implements AsyncService {

   private final static Logger logger = LoggerFactory.getLogger(AggregatorService.class);
   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;

   private static final String LOCATION_SEPERATOR = "-|-";

   @Override
   @Async("asyncServiceExecutor")
   public void executeAsync(EventMessage message) {
      if (message.getType() != EventType.Aggregator) {
         logger.warn("Drop none aggregator message " + message.getType());
         return;
      }
      logger.info(message.getContent());
      Set<EventUser> users = message.getTarget().getUsers();
      for (EventUser command : users) {
         logger.info(command.getId());
         switch (command.getId()) {
         case EventMessageUtil.FullMappingCommand:
            mergeServerMapping();
            logger.info("Finish full Mapping merge job.");
            break;
         case EventMessageUtil.HostNameIPMappingCommand:
            //the message in the format of:   hostname:ip
            String[] nameIP = message.getContent().split(":");
            syncHostMapping(nameIP[0], nameIP[1]);
            break;
         case EventMessageUtil.PDUServerMappingCommand:
            aggregateServerPDU();
            break;
         case EventMessageUtil.FullSyncTemperatureAndHumiditySensors:
            syncHostTemperatureAndHumidySensor(true);
            break;
         case EventMessageUtil.SyncTemperatureAndHumiditySensors:
            syncHostTemperatureAndHumidySensor(false);
            break;
         default:
            break;
         }
      }
   }

   public void mergeServerMapping() {
      //first get all vc
      //second get all vros
      // merge the data
      //TODO improve the efficiency of compare?
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      SDDCSoftwareConfig[] vcs = restClient.getVCServers().getBody();
      SDDCSoftwareConfig[] vrops = restClient.getVROServers().getBody();

      Map<String, ServerMapping> vroMapping = new HashMap<String, ServerMapping>();
      for (SDDCSoftwareConfig vro : vrops) {
         ServerMapping[] mappings = restClient.getServerMappingsByVRO(vro.getId()).getBody();
         for (ServerMapping mapping : mappings) {
            vroMapping.put(mapping.getVroVMEntityVCID() + ":" + mapping.getVroVMEntityObjectID(),
                  mapping);

         }
      }
      for (SDDCSoftwareConfig vc : vcs) {
         ServerMapping[] mappings = restClient.getServerMappingsByVC(vc.getId()).getBody();
         for (ServerMapping mapping : mappings) {
            String key = mapping.getVcInstanceUUID() + ":" + mapping.getVcMobID();
            if (vroMapping.containsKey(key)) {
               //check if they are same item.
               ServerMapping vroMap = vroMapping.get(key);
               if (!mapping.getId().equals(vroMap.getId())) {
                  //need to merge the mapping.
                  if (vroMap.getAsset() != null) {
                     restClient.mergMapping(vroMap.getId(), mapping.getId());
                  } else { //if (mapping.getAssetID() != null)   we should allow to merge unmapped mapping items.
                     restClient.mergMapping(mapping.getId(), vroMap.getId());
                  }
               }
            }
         }
      }
   }

   public void aggregateServerPDU() {
      /**
       * How do we know the relation between the PDU and servers? currently we can only assume that
       * base on the location information. If a server and pdu locate on the same rack. then they
       * have relations. This is not true for some case. eg. most rack have two pdu. some server
       * only connect to 1 pdu. In the long term, we should include the labsdb data which contain
       * the server,network, pdu conntion information. which is more accurate.
       *
       * Currently we can only call it "possible PDU" which means that the server may possiblely
       * connected to this PDU.
       */

      /**
       * Workflow: Filter out servers with empty PDU filed. Query all the PDUs and sort by Location.
       *
       * if the PDU and server has same location. Update the server's PDU information.
       *
       */
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      Asset[] incompletServers = restClient.getServersWithnoPDUInfo().getBody();
      if (incompletServers == null || incompletServers.length == 0) {
         return;
      }
      List<Asset> pdus = restClient.getAllAssetsByType(AssetCategory.PDU);

      // create map for PDUs base on the location.
      Map<String, List<Asset>> pduLookupMap = new HashMap<String, List<Asset>>();

      for (Asset pdu : pdus) {
         String key = getLocationIdentifier(pdu);
         if (pduLookupMap.containsKey(key)) {
            pduLookupMap.get(key).add(pdu);
         } else {
            List<Asset> list = new ArrayList<Asset>();
            list.add(pdu);
            pduLookupMap.put(key, list);
         }
      }
      List<Asset> toBeUpdatedAssets = new ArrayList<Asset>();
      for (Asset server : incompletServers) {
         String locationKey = getLocationIdentifier(server);
         if (pduLookupMap.containsKey(locationKey)) {
            List<String> assetPdus = new ArrayList<String>();
            for (Asset pdu : pduLookupMap.get(locationKey)) {
               assetPdus.add(pdu.getId());
            }
            server.setPdus(assetPdus);
            //update the pdu formula
            Map<ServerSensorType, String> formulas = server.getSensorsformulars();
            if (formulas == null) {
               formulas = new EnumMap<ServerSensorType, String>(ServerSensorType.class);
            }
            formulas.put(ServerSensorType.PDU_RealtimeLoad, assetPdus.get(0));//TODO we should consider multi pdu, and how to deal with it.
            formulas.put(ServerSensorType.PDU_RealtimePower, assetPdus.get(0));
            formulas.put(ServerSensorType.PDU_RealtimeVoltage, assetPdus.get(0));
            formulas.put(ServerSensorType.PDU_RealtimeLoadPercent, assetPdus.get(0));
            toBeUpdatedAssets.add(server);
         }
      }
      if (!toBeUpdatedAssets.isEmpty()) {
         restClient.saveAssets(toBeUpdatedAssets);
      }
   }

   private String getLocationIdentifier(Asset asset) {
      StringBuilder sb = new StringBuilder();
      sb.append(asset.getRegion()).append(LOCATION_SEPERATOR).append(asset.getCountry())
            .append(LOCATION_SEPERATOR).append(asset.getCity()).append(LOCATION_SEPERATOR)
            .append(asset.getBuilding()).append(LOCATION_SEPERATOR).append(asset.getFloor())
            .append(LOCATION_SEPERATOR).append(asset.getRoom()).append(LOCATION_SEPERATOR)
            .append(asset.getCabinetAssetNumber());
      return sb.toString();
   }

   private void syncHostMapping(String hostName, String serverMappingID) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      Asset asset = restClient.getAssetByName(hostName).getBody();
      if (null == asset) {
         logger.warn("Cannot find the host in the system: " + hostName);
         return;
      }
      //todo: we should try to find
   }

   /**
    * This task now focus on the Sensors from PowerIQ 1> First it get all the servers that has pdu.
    * 2> then it get all the sensors that attached with the pdu. 3> it update the server's sensor
    * information with the sensors get from step2
    */
   private void syncHostTemperatureAndHumidySensor(boolean fullSync) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      Asset[] allServers = restClient.getServersWithPDUInfo().getBody();
      List<Asset> candidateServer = new ArrayList<Asset>();
      if (fullSync) {
         candidateServer = Arrays.asList(allServers);
      } else {
         for (Asset asset : allServers) {
            Map<ServerSensorType, String> formulas = asset.getSensorsformulars();
            if (formulas == null) {
               formulas = new EnumMap<ServerSensorType, String>(ServerSensorType.class);
               asset.setSensorsformulars(formulas);
            }
            if (!formulas.containsKey(ServerSensorType.BACKPANELTEMP)
                  || formulas.containsKey(ServerSensorType.FRONTPANELTEMP)
                  || formulas.containsKey(ServerSensorType.HUMIDITY)) {
               candidateServer.add(asset);
            }
         }
      }

      if (candidateServer.isEmpty()) {
         return;
      }
      List<Asset> sensors = restClient.getAllAssetsByType(AssetCategory.Sensors);
      if (sensors.isEmpty()) {
         return;
      }
      Map<String, ArrayList<String>> temperatureSensorMap =
            new HashMap<String, ArrayList<String>>();
      Map<String, ArrayList<String>> humiditySensorMap = new HashMap<String, ArrayList<String>>();
      for (Asset sensor : sensors) {
         if (sensor.getJustificationfields() != null) {
            String pduAssetID = sensor.getJustificationfields().get(FlowgateConstant.PDU_ASSET_ID);
            if (pduAssetID != null) {
               switch (sensor.getSubCategory()) {
               case Temperature:
                  if (!temperatureSensorMap.containsKey(pduAssetID)) {
                     temperatureSensorMap.put(pduAssetID, new ArrayList<String>());
                  }
                  temperatureSensorMap.get(pduAssetID).add(sensor.getId());
                  break;
               case Humidity:
                  if (!humiditySensorMap.containsKey(pduAssetID)) {
                     humiditySensorMap.put(pduAssetID, new ArrayList<String>());
                  }
                  humiditySensorMap.get(pduAssetID).add(sensor.getId());
                  break;
               default:
                  break;
               }
            }
         }
      }

      //now start create the sensor/server mapping.
      List<Asset> needUpdateServers = new ArrayList<Asset>();
      for (Asset server : candidateServer) {
         List<String> pdus = server.getPdus();
         /**
          * bad assumption:each server have 2 pdus at most. We should change this.
          */
         String frontPanelSensor = null;
         String backPanelSensor = null;
         List<String> tempSensors = new ArrayList<String>();
         List<String> humiditySensors = new ArrayList<String>();
         boolean needUpdate=false;
         for (String pduID : pdus) {
            if (temperatureSensorMap.containsKey(pduID)) {
               tempSensors.addAll(temperatureSensorMap.get(pduID));
            }
            if(humiditySensorMap.containsKey(pduID)) {
               humiditySensors.addAll(humiditySensorMap.get(pduID));
            }
         }
         if (!tempSensors.isEmpty()) {
            frontPanelSensor = backPanelSensor = tempSensors.get(0);
            if (tempSensors.size() > 1) {
               backPanelSensor = tempSensors.get(1);
            }
            server.getSensorsformulars().put(ServerSensorType.FRONTPANELTEMP, frontPanelSensor);
            server.getSensorsformulars().put(ServerSensorType.BACKPANELTEMP, backPanelSensor);
            needUpdate = true;
         }
         if(!humiditySensors.isEmpty()) {
            server.getSensorsformulars().put(ServerSensorType.HUMIDITY, humiditySensors.get(0));
            needUpdate = true;
         }
         if(needUpdate) {
            needUpdateServers.add(server);
         }
      }
      if(!needUpdateServers.isEmpty()) {
         logger.info("update asset item number: "+needUpdateServers.size());
         restClient.saveAssets(needUpdateServers);
      }
   }

}
