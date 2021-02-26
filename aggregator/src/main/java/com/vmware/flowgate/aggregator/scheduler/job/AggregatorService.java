/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.aggregator.scheduler.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.ServerMapping;
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

   @Autowired
   StringRedisTemplate template;

   private static final String LOCATION_SEPERATOR = "-|-";
   private static final String extrnalTemperature = "extrnalTemperature";
   private static final String extrnalHumidity = "extrnalHumidity";
   private ObjectMapper mapper = new ObjectMapper();

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
            logger.info("Finish PDUServerMapping job.");
            break;
         case EventMessageUtil.FullSyncTemperatureAndHumiditySensors:
            syncHostTemperatureAndHumidySensor(true);
            break;
         case EventMessageUtil.SyncTemperatureAndHumiditySensors:
            syncHostTemperatureAndHumidySensor(false);
            break;
         case EventMessageUtil.CleanRealtimeData:
            cleanRealtimeData();
            break;
         case EventMessageUtil.AggregateAndCleanPowerIQPDU:
            integrateAndCleanPDUFromPowerIQ();
            break;
         case EventMessageUtil.SUMMARY_DATA:
            syncSummaryData();
            break;
         default:
            break;
         }
      }
   }

   public void syncSummaryData() {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      restClient.getSystemSummary(false);
      logger.info("Finish sync system summary data.");
   }

   private void cleanRealtimeData() {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      long expiredTimeRange = 0l;
      String expiredTimeRangeValue = template.opsForValue().get(EventMessageUtil.EXPIREDTIMERANGE);
      if(expiredTimeRangeValue != null) {
         expiredTimeRange = Long.valueOf(expiredTimeRangeValue);
      }else {
         expiredTimeRange = FlowgateConstant.DEFAULTEXPIREDTIMERANGE;
      }
      restClient.deleteRealTimeData(expiredTimeRange);
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

   public void integrateAndCleanPDUFromPowerIQ() {
      logger.info("Start aggregate pdu from PowerIQ to other systems");
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      FacilitySoftwareConfig[] powerIQs = restClient.getFacilitySoftwareInternalByType(SoftwareType.PowerIQ).getBody();
      if(powerIQs ==null || powerIQs.length==0) {
         logger.info("No PowerIQ server find");
         return;
      }
      Map<String,Asset> pdusOnlyFromPowerIQ = new HashMap<String,Asset>();
      Map<String,String> powerIQIDs = new HashMap<String,String>();
      for(FacilitySoftwareConfig powerIQ : powerIQs) {
         powerIQIDs.put(powerIQ.getId(),powerIQ.getName());
      }
      if(powerIQIDs.isEmpty()) {
         logger.info("No Pdu from PowerIQ server find");
         return;
      }
      List<Asset> pdus = restClient.getAllAssetsByType(AssetCategory.PDU);
      Iterator<Asset> pduIte = pdus.iterator();
      while(pduIte.hasNext()) {
         Asset pdu = pduIte.next();
         //pdu only from powerIQ
         if (pdu.getAssetSource().split(FlowgateConstant.SPILIT_FLAG).length == 1 && powerIQIDs.get(pdu.getAssetSource()) != null) {
            if(pdu.getAssetName() == null || pdu.getAssetName().isEmpty()) {
               continue;
            }
            pdusOnlyFromPowerIQ.put(pdu.getAssetName().toLowerCase(), pdu);
            pduIte.remove();
         }
      }
      if(pdus.isEmpty()) {
         logger.info("All pdus are from powerIQ");
         return;
      }
      HashSet<String> pduAssetIds = new HashSet<String>(pdusOnlyFromPowerIQ.size());
      /**
       * Part of these pdus comes from other DCIMs,
       * and the other part comes from PowerIQ and other DCIMs(integrated pdus)
       * We need NON-PowerIQ's pdus(Not integrated pdus)
       */
      for(Asset pdu : pdus) {
         if(pdu.getAssetName() == null || pdu.getAssetName().isEmpty()) {
            continue;
         }
         String pduName = pdu.getAssetName().toLowerCase();
         String source = pdu.getAssetSource();
         boolean isSkip = false;
         //pdus from multiple DCIM system
         if(source.indexOf(FlowgateConstant.SPILIT_FLAG) > -1) {
            String[] sources = source.split(FlowgateConstant.SPILIT_FLAG);
            for(String assetSource : sources) {
               if(powerIQIDs.containsKey(assetSource)) {
                  //Remove integrated pdu
                  pdusOnlyFromPowerIQ.remove(pduName);
                  isSkip = true;
                  break;
               }
            }
         }
         /**
          * When the pdu is integrated, skip.
          */
         if(isSkip) {
            continue;
         }
         //We need to integrate the PowerIQ PDU and Other DCIM PDU
         Asset pduFromPowerIQ = pdusOnlyFromPowerIQ.get(pduName);
         if(pduFromPowerIQ != null) {
            HashMap<String,String> pduFromPowerIQExtraInfo = pduFromPowerIQ.getJustificationfields();
            HashMap<String,String> pduExtraInfo = pdu.getJustificationfields();

            if(source.indexOf(pduFromPowerIQ.getAssetSource()) == -1) {
               pdu.setAssetSource(source + FlowgateConstant.SPILIT_FLAG + pduFromPowerIQ.getAssetSource());
            }
            Map<String, String> metricsformulas = pduFromPowerIQ.getMetricsformulas();
            if(!metricsformulas.isEmpty()) {
               pdu.setMetricsformulas(metricsformulas);
            }
            if(pduExtraInfo == null || pduExtraInfo.isEmpty()) {
               pdu.setJustificationfields(pduFromPowerIQExtraInfo);
               restClient.saveAssets(pdu);
               pduAssetIds.add(pduFromPowerIQ.getId());
               //If there are more than one pdus with the same name from Nlyte system,only one from these pdus can be merged.
               pdusOnlyFromPowerIQ.remove(pduName);
               restClient.removeAssetByID(pduFromPowerIQ.getId());
               continue;
            }
            String pduInfo = pduFromPowerIQExtraInfo.get(FlowgateConstant.PDU);
            if(pduInfo == null) {
               continue;
            }
            String oldPduInfo = pduExtraInfo.get(FlowgateConstant.PDU);
            if(oldPduInfo == null) {
               pduExtraInfo.put(FlowgateConstant.PDU, pduInfo);
               restClient.saveAssets(pdu);
               pduAssetIds.add(pduFromPowerIQ.getId());
               pdusOnlyFromPowerIQ.remove(pduName);
               restClient.removeAssetByID(pduFromPowerIQ.getId());
               continue;
            }
            Map<String,String> pduInfoMap = null;
            Map<String,String> oldPduInfoMap = null;
            try {
               pduInfoMap = mapper.readValue(pduInfo, new TypeReference<Map<String,String>>() {});
               oldPduInfoMap = mapper.readValue(oldPduInfo, new TypeReference<Map<String,String>>() {});
            } catch (IOException e) {
               logger.error("Format pdu justficationfields error");
               continue;
            }
            oldPduInfoMap.put(FlowgateConstant.PDU_RATE_AMPS, pduInfoMap.get(FlowgateConstant.PDU_RATE_AMPS));
            oldPduInfoMap.put(FlowgateConstant.PDU_MIN_RATE_POWER, pduInfoMap.get(FlowgateConstant.PDU_MIN_RATE_POWER));
            oldPduInfoMap.put(FlowgateConstant.PDU_MAX_RATE_POWER, pduInfoMap.get(FlowgateConstant.PDU_MAX_RATE_POWER));
            oldPduInfoMap.put(FlowgateConstant.PDU_MIN_RATE_VOLTS, pduInfoMap.get(FlowgateConstant.PDU_MIN_RATE_VOLTS));
            oldPduInfoMap.put(FlowgateConstant.PDU_MAX_RATE_VOLTS, pduInfoMap.get(FlowgateConstant.PDU_MAX_RATE_VOLTS));
            oldPduInfoMap.put(FlowgateConstant.PDU_OUTLETS_FROM_POWERIQ, pduInfoMap.get(FlowgateConstant.PDU_OUTLETS_FROM_POWERIQ));
            oldPduInfoMap.put(FlowgateConstant.PDU_INLETS_FROM_POWERIQ, pduInfoMap.get(FlowgateConstant.PDU_INLETS_FROM_POWERIQ));
            oldPduInfoMap.put(FlowgateConstant.PDU_ID_FROM_POWERIQ, pduInfoMap.get(FlowgateConstant.PDU_ID_FROM_POWERIQ));
            try {
               String newPduInfo = mapper.writeValueAsString(oldPduInfoMap);
               pduExtraInfo.put(FlowgateConstant.PDU, newPduInfo);
               pdu.setJustificationfields(pduExtraInfo);
            } catch (JsonProcessingException e) {
               logger.error("Format pdu extra info error",e.getCause());
            }
            restClient.saveAssets(pdu);
            pduAssetIds.add(pduFromPowerIQ.getId());
            pdusOnlyFromPowerIQ.remove(pduName);
            restClient.removeAssetByID(pduFromPowerIQ.getId());
         }
      }
      logger.info("Finished aggregate pdu from PowerIQ to other systems");

      if(!pduAssetIds.isEmpty()) {
         Asset[] serversWithPduInfo = restClient.getServersWithPDUInfo().getBody();
         if(serversWithPduInfo== null || serversWithPduInfo.length == 0) {
            logger.info("No mapped server");
            return;
         }
        List<Asset> needToUpdateServer = removePduFromServer(serversWithPduInfo,pduAssetIds);
        restClient.saveAssets(needToUpdateServer);
      }
   }

   public List<Asset> removePduFromServer(Asset[] servers, HashSet<String> removedPduIds) {
      List<Asset> needToUpdate = new ArrayList<Asset>();
      for(Asset server : servers) {
         boolean changed = false;
         List<String> pduIds = server.getPdus();
         Iterator<String> pduite = pduIds.iterator();
         while(pduite.hasNext()) {
            String pduid = pduite.next();
            if(removedPduIds.contains(pduid)) {
               pduite.remove();
               changed = true;
            }
         }
         server.setPdus(pduIds);

         if(changed) {
            HashMap<String, String> serverJustficationfields = server.getJustificationfields();
            String pduPortString = serverJustficationfields.get(FlowgateConstant.PDU_PORT_FOR_SERVER);
            if(pduPortString != null) {
               String pduPorts[] = pduPortString.split(FlowgateConstant.SPILIT_FLAG);
               List<String> pduPortsList = new ArrayList<String>(Arrays.asList(pduPorts));
               Iterator<String> portsIte = pduPortsList.iterator();
               while (portsIte.hasNext()) {
                 String pduport = portsIte.next();
                 String pduAssetId = pduport.substring(pduport.lastIndexOf(FlowgateConstant.SEPARATOR) + FlowgateConstant.SEPARATOR.length());
                 if(removedPduIds.contains(pduAssetId)) {
                    portsIte.remove();
                 }
               }
               if(pduPortsList.isEmpty()) {
                  pduPortString =  null;
               }else {
                  pduPortString = String.join(FlowgateConstant.SPILIT_FLAG, pduPortsList);
               }
               serverJustficationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, pduPortString);
               server.setJustificationfields(serverJustficationfields);
            }
            Map<String, String> formulas = server.getMetricsformulas();
            if(formulas != null && formulas.get(FlowgateConstant.PDU) != null) {
               String pduFormulasInfo = formulas.get(FlowgateConstant.PDU);
               Map<String, Map<String, String>> pduFormulasMap = metricsFormulaToMap(pduFormulasInfo);
               Iterator<Map.Entry<String, Map<String, String>>> ite = pduFormulasMap.entrySet().iterator();
               while(ite.hasNext()) {
                  Map.Entry<String, Map<String, String>> map = ite.next();
                  String pduAssetID = map.getKey();
                  if (removedPduIds.contains(pduAssetID)) {
                     ite.remove();
                  }
                }
               String pduFormulaInfo = metricsFormulatoString(pduFormulasMap);
               formulas.put(FlowgateConstant.PDU, pduFormulaInfo);
               server.setMetricsformulas(formulas);
            }
            needToUpdate.add(server);
         }
      }
      return needToUpdate;
   }

   public Map<String,Map<String,String>> generatePduformulaForServer (List<String> pduAssetIds){
      Map<String,Map<String,String>> pduFormula = new HashMap<String,Map<String,String>>();
      for(String pduAssetId : pduAssetIds) {
         Map<String,String> metricNameAndIdMap = new HashMap<String,String>();
         metricNameAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_CURRENT, pduAssetId);
         metricNameAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_POWER, pduAssetId);
         metricNameAndIdMap.put(MetricName.SERVER_USED_PDU_OUTLET_CURRENT, pduAssetId);
         metricNameAndIdMap.put(MetricName.SERVER_USED_PDU_OUTLET_POWER, pduAssetId);
         metricNameAndIdMap.put(MetricName.SERVER_VOLTAGE, pduAssetId);
         pduFormula.put(pduAssetId, metricNameAndIdMap);
      }
      return pduFormula;
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
      /**
       * Get all servers with pdus is not null which are mapped with IT systems.
       */
      Asset[] incompletServers = restClient.getServersWithnoPDUInfo().getBody();
      if (incompletServers != null && incompletServers.length != 0) {
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
               List<String> assetPduIds = new ArrayList<String>();
               for (Asset pdu : pduLookupMap.get(locationKey)) {
                  assetPduIds.add(pdu.getId());
               }
               server.setPdus(assetPduIds);
               toBeUpdatedAssets.add(server);
            }
         }
         if (!toBeUpdatedAssets.isEmpty()) {
            restClient.saveAssets(toBeUpdatedAssets);
         }
      }

      //Update metricsFormular for server by pdus
      Asset[] servers = restClient.getServersWithPDUInfo().getBody();
      /**
       * Generate pdu metricsFormular for server,
       * the pdus field of server is generated by other facility system.
       * We need to create some metricsFormular to store the relations between metrics and PDUs.
       */
      List<Asset> needToSaveServers = new ArrayList<Asset>();
      for(Asset server : servers) {
         Map<String, String> metricsFormulas = server.getMetricsformulas();
         List<String> pduIds = server.getPdus();
         if(metricsFormulas == null || metricsFormulas.isEmpty()) {
            metricsFormulas = new HashMap<String, String>();
            Map<String,Map<String,String>> pduFormulas = generatePduformulaForServer(pduIds);
            String pduFormulaInfo = metricsFormulatoString(pduFormulas);
            if(pduFormulaInfo != null) {
               metricsFormulas.put(FlowgateConstant.PDU, pduFormulaInfo);
               server.setMetricsformulas(metricsFormulas);
               needToSaveServers.add(server);
            }
         }else {
            String pduFormulaInfo = metricsFormulas.get(FlowgateConstant.PDU);
            if(pduFormulaInfo == null) {
               Map<String,Map<String,String>> pduFormulas = generatePduformulaForServer(pduIds);
               pduFormulaInfo = metricsFormulatoString(pduFormulas);
               if(pduFormulaInfo != null) {
                  metricsFormulas.put(FlowgateConstant.PDU, pduFormulaInfo);
                  server.setMetricsformulas(metricsFormulas);
                  needToSaveServers.add(server);
               }
            }else {
               boolean isNeedUpdated = false;
               Map<String, Map<String, String>> pduFormulasMap = metricsFormulaToMap(pduFormulaInfo);
               if(pduFormulasMap != null) {
                  if(pduIds.size() != pduFormulasMap.keySet().size()) {
                     pduFormulasMap = generatePduformulaForServer(pduIds);
                     isNeedUpdated = true;
                  }else {
                     for(String pduAssetId : pduIds) {
                        if(pduFormulasMap.get(pduAssetId) == null) {
                           pduFormulasMap = generatePduformulaForServer(pduIds);
                           isNeedUpdated = true;
                        }
                     }
                  }
               }
               if(isNeedUpdated) {
                  pduFormulaInfo = metricsFormulatoString(pduFormulasMap);
                  if(pduFormulaInfo != null) {
                     metricsFormulas.put(FlowgateConstant.PDU, pduFormulaInfo);
                     server.setMetricsformulas(metricsFormulas);
                     needToSaveServers.add(server);
                  }
               }
            }
         }
      }
      if(!needToSaveServers.isEmpty()) {
         restClient.saveAssets(needToSaveServers);
         logger.info("Finished aggregate pdu for server");
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
            Map<String, String> metricsFormulas = asset.getMetricsformulas();
            if (metricsFormulas == null) {
               metricsFormulas = new HashMap<String, String>();
               asset.setMetricsformulas(metricsFormulas);
               candidateServer.add(asset);
            } else {
               String sensorFormulasInfo = metricsFormulas.get(FlowgateConstant.SENSOR);
               if (sensorFormulasInfo == null) {
                  candidateServer.add(asset);
               } else {
                  Map<String, Map<String, String>> metricFormulaMap = metricsFormulaToMap(sensorFormulasInfo);
                  if(metricFormulaMap == null) {
                     continue;
                  }
                  if (!metricFormulaMap.containsKey(MetricName.SERVER_BACK_TEMPREATURE)
                        || !metricFormulaMap.containsKey(MetricName.SERVER_FRONT_TEMPERATURE)
                        || !metricFormulaMap.containsKey(MetricName.SERVER_BACK_HUMIDITY)
                        || !metricFormulaMap.containsKey(MetricName.SERVER_FRONT_HUMIDITY)) {
                     candidateServer.add(asset);
                  }
               }
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
      Map<String,List<Asset>> pduAndSensorsMap = new HashMap<String,List<Asset>>();
      for (Asset sensor : sensors) {
         if (sensor.getJustificationfields() == null) {
            continue;
         }
         String sensorInfo = sensor.getJustificationfields().get(FlowgateConstant.SENSOR);
         if(sensorInfo == null) {
            continue;
         }
         Map<String, String> sensorInfoMap = null;
         try {
            sensorInfoMap = mapper.readValue(sensorInfo, new TypeReference<Map<String,String>>() {});
         }  catch (IOException e) {
            logger.error("Format sensor info map error ",e.getMessage());
            continue;
         }
         String pduAssetID = sensorInfoMap.get(FlowgateConstant.PDU_ASSET_ID);
         if(pduAssetID == null) {
            continue;
         }
         if (!pduAndSensorsMap.containsKey(pduAssetID)) {
            pduAndSensorsMap.put(pduAssetID, new ArrayList<Asset>());
         }
         pduAndSensorsMap.get(pduAssetID).add(sensor);
      }

      List<Asset> needUpdateServers = new ArrayList<Asset>();
      for (Asset server : candidateServer) {
         List<String> pduIds = server.getPdus();
         List<String> temperatureSensorAssetIds = new ArrayList<String>();
         List<String> humiditySensorAssetIds = new ArrayList<String>();
         boolean needUpdate = false;
         Map<String, String> metricsFormulas = server.getMetricsformulas();
         String sensorMetricFormulasInfo = null;
         Map<String,Map<String,String>> sensorMetricsFormulasMap = null;
         if(metricsFormulas != null) {
            sensorMetricFormulasInfo = metricsFormulas.get(FlowgateConstant.SENSOR);
         }
         if(sensorMetricFormulasInfo == null) {
            sensorMetricsFormulasMap = new HashMap<String,Map<String,String>>();
         }else {
            sensorMetricsFormulasMap = metricsFormulaToMap(sensorMetricFormulasInfo);
            //If there is a problem with deserialization, the map will be null
            if(sensorMetricsFormulasMap == null) {
               continue;
            }
         }
         List<Asset> allSensorAssetsForServer = new ArrayList<Asset>();
         for (String pduID : pduIds) {
            List<Asset> sensorAssets = pduAndSensorsMap.get(pduID);
            if(sensorAssets == null) {
               continue;
            }
            allSensorAssetsForServer.addAll(sensorAssets);
         }
         if(allSensorAssetsForServer.isEmpty()) {
            continue;
         }
         generateMetricsFormular(sensorMetricsFormulasMap, allSensorAssetsForServer,
               temperatureSensorAssetIds, humiditySensorAssetIds);

         if(sensorMetricsFormulasMap.isEmpty()) {
            if(!temperatureSensorAssetIds.isEmpty()) {
               Map<String,String> frontTemp = new HashMap<String,String>();
               frontTemp.put(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION, temperatureSensorAssetIds.get(0));
               sensorMetricsFormulasMap.put(MetricName.SERVER_FRONT_TEMPERATURE, frontTemp);

               Map<String,String> backTemp = new HashMap<String,String>();
               String backTemperatureId = temperatureSensorAssetIds.get(0);
               if(temperatureSensorAssetIds.size() > 1) {
                  backTemperatureId = temperatureSensorAssetIds.get(1);
               }
               backTemp.put(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION, backTemperatureId);
               sensorMetricsFormulasMap.put(MetricName.SERVER_BACK_TEMPREATURE, backTemp);

               needUpdate = true;
            }
            if(!humiditySensorAssetIds.isEmpty()) {
               Map<String,String> humidity = new HashMap<String,String>();
               humidity.put(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION, humiditySensorAssetIds.get(0));
               sensorMetricsFormulasMap.put(MetricName.SERVER_FRONT_HUMIDITY, humidity);

               Map<String,String> backHumidity = new HashMap<String,String>();
               String backHumidityId = humiditySensorAssetIds.get(0);
               if(humiditySensorAssetIds.size() > 1) {
                  backHumidityId = humiditySensorAssetIds.get(1);
               }
               backHumidity.put(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION, backHumidityId);
               sensorMetricsFormulasMap.put(MetricName.SERVER_BACK_HUMIDITY, backHumidity);
               needUpdate = true;
            }
         }else {
            needUpdate = true;
         }
         if(needUpdate) {
            String sensorMetricsFormulaInfo = metricsFormulatoString(sensorMetricsFormulasMap);
            metricsFormulas.put(FlowgateConstant.SENSOR, sensorMetricsFormulaInfo);
            needUpdateServers.add(server);
         }
      }

      if(!needUpdateServers.isEmpty()) {
         logger.info("update asset item number: "+needUpdateServers.size());
         restClient.saveAssets(needUpdateServers);
      }
      logger.info("No server need to update sensor metric formula.");
   }

   public void generateMetricsFormular(Map<String,Map<String,String>> metricsNameAndSensorsMap,
         List<Asset> sensorAssets, List<String> temperatureSensorAssetIds, List<String> humiditySensorAssetIds){
      for(Asset sensor : sensorAssets) {
         String positionInfo = getPositionInfo(sensor);
         switch (sensor.getMountingSide()) {
         case Front:
            switch (sensor.getSubCategory()) {
            case Temperature:
               fillingData(metricsNameAndSensorsMap, MetricName.SERVER_FRONT_TEMPERATURE,
                     positionInfo, sensor.getId());
               break;
            case Humidity:
               fillingData(metricsNameAndSensorsMap, MetricName.SERVER_FRONT_HUMIDITY,
                     positionInfo, sensor.getId());
               break;
            default:
               break;
            }
            break;
         case Back:
            switch (sensor.getSubCategory()) {
            case Temperature:
               fillingData(metricsNameAndSensorsMap, MetricName.SERVER_BACK_TEMPREATURE,
                     positionInfo, sensor.getId());
               break;
            case Humidity:
               fillingData(metricsNameAndSensorsMap, MetricName.SERVER_BACK_HUMIDITY,
                     positionInfo, sensor.getId());
               break;
            default:
               break;
            }
            break;
         case External:
         case Unmounted:
            /**
             * Save these sensor asset which are external or unmounted,
             * When the server's metricsFormula is empty, we will use these sensor assets to create a metrics formula.
             */
            switch (sensor.getSubCategory()) {
            case Temperature:
               temperatureSensorAssetIds.add(sensor.getId());
               break;
            case Humidity:
               humiditySensorAssetIds.add(sensor.getId());
               break;
            default:
               break;
            }
            break;
         default:
            break;
         }
      }
   }

   public void fillingData(Map<String,Map<String,String>> metricsNameAndSensorsMap, String metricName,
         String positionInfo, String sensorAssetId) {
      Map<String,String> metricLocationAndAssetIdMap  = metricsNameAndSensorsMap.get(metricName);
      if(metricLocationAndAssetIdMap == null) {
         metricLocationAndAssetIdMap = new HashMap<String,String>();
      }
      metricLocationAndAssetIdMap.put(positionInfo, sensorAssetId);
      metricsNameAndSensorsMap.put(metricName, metricLocationAndAssetIdMap);
   }

   public String getPositionInfo(Asset asset) {
      StringBuilder positionInfo = new StringBuilder();
      Map<String,String> sensorAssetJustfication = asset.getJustificationfields();
      int rackUnitNumber = asset.getCabinetUnitPosition();
      String rackUnitInfo = null;
      String positionFromAsset = null;

      if(rackUnitNumber != 0) {
         rackUnitInfo = FlowgateConstant.RACK_UNIT_PREFIX  + rackUnitNumber;
         positionInfo.append(rackUnitInfo);
         if(sensorAssetJustfication == null || sensorAssetJustfication.isEmpty() ||
               sensorAssetJustfication.get(FlowgateConstant.SENSOR) == null) {
            return positionInfo.toString();
         }
         String sensorInfo = sensorAssetJustfication.get(FlowgateConstant.SENSOR);
         try {
            Map<String,String> sensorInfoMap = mapper.readValue(sensorInfo, new TypeReference<Map<String,String>>() {});
            positionFromAsset = sensorInfoMap.get(FlowgateConstant.POSITION);
            if(positionFromAsset != null) {
               positionInfo.append(FlowgateConstant.SEPARATOR + positionFromAsset);
            }
         } catch (IOException e) {
            return positionInfo.toString();
         }
      }else {
         if(sensorAssetJustfication == null || sensorAssetJustfication.isEmpty() ||
               sensorAssetJustfication.get(FlowgateConstant.SENSOR) == null) {
            positionInfo.append(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION);
            return positionInfo.toString();
         }
         String sensorInfo = sensorAssetJustfication.get(FlowgateConstant.SENSOR);
         try {
            Map<String,String> sensorInfoMap = mapper.readValue(sensorInfo, new TypeReference<Map<String,String>>() {});
            positionFromAsset = sensorInfoMap.get(FlowgateConstant.POSITION);
            if(positionFromAsset != null) {
               positionInfo.append(positionFromAsset);
            }else {
               positionInfo.append(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION);
            }
         } catch (IOException e) {
            positionInfo.append(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION);
            return positionInfo.toString();
         }
      }
      return positionInfo.toString();
   }

   private String metricsFormulatoString(Map<String, Map<String, String>> pduFormulasMap) {
      String pduFormulaInfo = null;
      try {
         pduFormulaInfo = mapper.writeValueAsString(pduFormulasMap);
      } catch (JsonProcessingException e) {
         logger.error("Format metric formula error ",e.getMessage());
      }
      return pduFormulaInfo;
   }

   private Map<String, Map<String, String>> metricsFormulaToMap(String pduFormulasInfo){
      Map<String, Map<String, String>> pduFormulasMap = null;
      try {
         pduFormulasMap = mapper.readValue(pduFormulasInfo, new TypeReference<Map<String, Map<String, String>>>() {});
      }  catch (IOException e) {
         logger.error("Format metric formula error ",e.getMessage());
      }
      return pduFormulasMap;
   }

}
