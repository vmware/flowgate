/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker.scheduler.job;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricKeyName;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.MetricData;
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
import com.vmware.flowgate.vroworker.config.ServiceKeyConfig;
import com.vmware.flowgate.vroworker.vro.AlertClient;
import com.vmware.flowgate.vroworker.vro.MetricClient;
import com.vmware.flowgate.vroworker.vro.VROConfig;
import com.vmware.flowgate.vroworker.vro.VROConsts;
import com.vmware.ops.api.client.exceptions.AuthException;
import com.vmware.ops.api.model.property.PropertyContent;
import com.vmware.ops.api.model.property.PropertyContents;
import com.vmware.ops.api.model.resource.ResourceDto;
import com.vmware.ops.api.model.resource.ResourceIdentifier;
import com.vmware.ops.api.model.stat.StatContent;
import com.vmware.ops.api.model.stat.StatContents;

@Service
public class VROAsyncJob implements AsyncService {

   private static final Logger logger = LoggerFactory.getLogger(VROAsyncJob.class);
   @Autowired
   private WormholeAPIClient restClient;
   @Autowired
   private ServiceKeyConfig serviceKeyConfig;
   @Autowired
   private MessagePublisher publisher;

   @Autowired
   private StringRedisTemplate template;

   private ObjectMapper mapper = new ObjectMapper();

   private static final Map<String, String> methodPropertyMapping;
   private static final int HTTP_NOTFOUND = 404;
   static {
      Map<String, String> kvMap = new HashMap<String, String>();
      kvMap.put("region", VROConsts.LOCATION_REGION);
      kvMap.put("country", VROConsts.LOCATION_COUNTRY);
      kvMap.put("city", VROConsts.LOCATION_CITY);
      kvMap.put("building", VROConsts.LOCATION_BUILDING);
      kvMap.put("floor", VROConsts.LOCATION_FLOOR);
      kvMap.put("room", VROConsts.LOCATION_ROOM);
      kvMap.put("row", VROConsts.LOCATION_ROW);
      kvMap.put("col", VROConsts.LOCATION_COL);
      kvMap.put("cabinetName", VROConsts.LOCATION_CABINET);
      kvMap.put("cabinetUnitPosition", VROConsts.LOCATION_CABINET_NUMBER);
      methodPropertyMapping = Collections.unmodifiableMap(kvMap);
   }
   private static int executionCount = 0;
   private static HashMap<String, Long> latencyFactorMap = new HashMap<String, Long>();
   private static HashMap<String, Long> lastUpdateTimeMap = new HashMap<String, Long>();

   private static final String EntityName = "VMEntityName";
   private static final String VMEntityObjectID = "VMEntityObjectID";
   private static final String VMEntityVCID = "VMEntityVCID";
   private static long FIVE_MINUTES = 60 * 5 * 1000;

   @Override
   @Async("asyncServiceExecutor")
   public void executeAsync(EventMessage message) {
      if (message.getType() != EventType.VROps) {
         logger.warn("Drop none VROps message " + message.getType());
         return;
      }
      //TO, this should be comment out since it may contain vc password.
      logger.info("message received");
      Set<EventUser> users = message.getTarget().getUsers();

      for (EventUser command : users) {
         logger.info(command.getId());
         switch (command.getId()) {
         case EventMessageUtil.VRO_SyncData:
            String messageString = null;
            while ((messageString =
                  template.opsForList().rightPop(EventMessageUtil.vroJobList)) != null) {
               EventMessage payloadMessage = null;
               try {
                  payloadMessage = mapper.readValue(messageString, EventMessageImpl.class);
               } catch (IOException e) {
                  logger.error("Cannot process message", e);
               }
               if (payloadMessage == null) {
                  continue;
               }
               SDDCSoftwareConfig vroInfo = null;
               try {
                  vroInfo = mapper.readValue(payloadMessage.getContent(), SDDCSoftwareConfig.class);
               } catch (IOException e) {
                  logger.error("Cannot process message", e);
               }
               if (null == vroInfo) {
                  continue;
               }
               for (EventUser payloadCommand : payloadMessage.getTarget().getUsers()) {
                  switch (payloadCommand.getId()) {
                  case EventMessageUtil.VRO_SyncMetricData:
                     syncVROMetricData(vroInfo);
                     logger.info("Finish Sync Metric data for " + vroInfo.getName());
                     break;
                  case EventMessageUtil.VRO_SyncMetricPropertyAndAlert:
                     syncVROMetricPropertyAlertDefinition(vroInfo);
                     logger.info(
                           "Finish Sync customer attributes and alerts for " + vroInfo.getName());
                     break;
                  default:
                     break;
                  }
               }
            }
            break;
         case EventMessageUtil.VRO_SyncMetricData:
            logger.warn("VRO_SyncMetricData command is deprecated, please use VRO_SyncData");
            break;
         case EventMessageUtil.VRO_SyncMetricPropertyAndAlert:
            logger.warn(
                  "VRO_SyncMetricPropertyAndAlert command is deprecated, please use VRO_SyncData");
            break;
         default:
            logger.warn("Unknown command, ignore it: " + command.getId());
            break;
         }
      }
   }

   public void updateIntegrationStatus(SDDCSoftwareConfig config) {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      restClient.updateSDDC(config);
   }

   private void syncVROMetricData(SDDCSoftwareConfig config) {
      if (!config.checkIsActive()) {
         return;
      }
      VROConfig vro = new VROConfig(config);
      long currentTime = System.currentTimeMillis();
      MetricClient metricClient = null;
      List<ResourceDto> hosts = null;
      try {
         metricClient = new MetricClient(vro, publisher);
         hosts = metricClient.getHostSystemsResources();
      } catch (AuthException authException) {
         logger.error("Failed to connect to VROps manager ", authException);
         IntegrationStatus integrationStatus = config.getIntegrationStatus();
         if (integrationStatus == null) {
            integrationStatus = new IntegrationStatus();
         }
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(authException.getMessage());
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         updateIntegrationStatus(config);
         return;
      } catch (Exception e) {
         if(e.getCause() instanceof UndeclaredThrowableException) {
            UndeclaredThrowableException undeclaredThrowableException = (UndeclaredThrowableException)e.getCause();
            if (undeclaredThrowableException.getUndeclaredThrowable().getCause() instanceof ConnectException) {
               checkAndUpdateIntegrationStatus(config,
                     undeclaredThrowableException.getUndeclaredThrowable().getCause().getMessage());
               return;
            }
         }
         throw e;
      }
      //read all host/asset maaping from apiserver
      Map<String, Asset> assetDictionary = new HashMap<String, Asset>();
      try {
         restClient.setServiceKey(serviceKeyConfig.getServiceKey());
         Asset[] servers = restClient.getAssetsByVRO(vro.getId()).getBody();
         for (Asset server : servers) {
            assetDictionary.put(server.getId(), server);
         }
      } catch (HttpClientErrorException clientError) {
         if (clientError.getRawStatusCode() != HTTP_NOTFOUND) {
            throw clientError;
         }
      }
      ServerMapping[] mappings = null;
      try {
         mappings = restClient.getServerMappingsByVRO(vro.getId()).getBody();
      } catch (HttpClientErrorException clientError) {
         if (clientError.getRawStatusCode() != HTTP_NOTFOUND) {
            throw clientError;
         }
         mappings = new ServerMapping[0];
      }
      HashMap<String, ServerMapping> entityNameDictionary = new HashMap<String, ServerMapping>();
      HashMap<String, ServerMapping> objectIDDictionary = new HashMap<String, ServerMapping>();
      //HashMap<String, ServerMapping> vcIDDictionary = new HashMap<String, ServerMapping>();

      for (ServerMapping mapping : mappings) {
         entityNameDictionary.put(mapping.getVroVMEntityName(), mapping);
         objectIDDictionary.put(mapping.getVroVMEntityObjectID(), mapping);
         //vcIDDictionary.put(mapping.getVroVMEntityVCID(), mapping);
      }
      List<ServerMapping> validMapping = new ArrayList<ServerMapping>();
      for (ResourceDto host : hosts) {
         //There are several things that can be used as a identifier of a host.
         //the first is the Name
         //beside that there are three other identifier
         //VMEntityName -- 10.112.113.160
         //VMEntityObjectID  host-81  the mobID of the host.
         //VMEntityVCID  this is the uuid of the vc object.
         //currently we only add new servers and don't delete servers

         List<ResourceIdentifier> identifiers = host.getResourceKey().getResourceIdentifiers();
         boolean newHost = true;
         String entityName = "";
         String objectID = "";
         String vcID = "";
         ServerMapping refer = null;
         for (ResourceIdentifier identifier : identifiers) {
            if (!newHost) {
               break;
            }
            switch (identifier.getIdentifierType().getName()) {
            case EntityName:
               entityName = identifier.getValue();
               if (entityNameDictionary.containsKey(entityName)) {
                  newHost = false;
                  refer = entityNameDictionary.get(entityName);
               }
               break;
            case VMEntityObjectID:
               objectID = identifier.getValue();
               if (objectIDDictionary.containsKey(objectID)) {
                  newHost = false;
                  refer = objectIDDictionary.get(objectID);
               }
               break;
            case VMEntityVCID:
               vcID = identifier.getValue();
               break;
            default:
               break;
            }
         }
         if (newHost) {
            //try to notify the other system.
            String ipaddress = IPAddressUtil.getIPAddress(entityName);
            if (null != ipaddress) {
               publisher.publish(null, ipaddress);
               ipaddress = entityName;
            }
            ServerMapping newMapping = new ServerMapping();
            newMapping.setVroID(vro.getId());
            newMapping.setVroResourceName(host.getResourceKey().getName());
            newMapping.setVroVMEntityName(entityName);
            newMapping.setVroVMEntityObjectID(objectID);
            newMapping.setVroVMEntityVCID(vcID);
            newMapping.setVroResourceID(host.getIdentifier().toString());
            AssetIPMapping[] ipMappings = restClient.getHostnameIPMappingByIP(ipaddress).getBody();
            if (null != ipMappings && ipMappings.length > 0) {
               //update the mapping
               String assetName = ipMappings[0].getAssetname();
               Asset asset = restClient.getAssetByName(assetName).getBody();
               if (asset != null) {
                  newMapping.setAsset(asset.getId());
               }
            }
            restClient.saveServerMapping(newMapping);
            validMapping.add(newMapping);
         } else {
            if (refer.getAsset() == null) {
               String ipaddress = IPAddressUtil.getIPAddress(refer.getVroVMEntityName());
               AssetIPMapping[] ipMappings =
                     restClient.getHostnameIPMappingByIP(ipaddress).getBody();
               if (null != ipMappings && ipMappings.length > 0) {
                  //update the mapping
                  String assetName = ipMappings[0].getAssetname();
                  Asset asset = restClient.getAssetByName(assetName).getBody();
                  if (asset != null) {
                     refer.setAsset(asset.getId());
                     restClient.saveServerMapping(refer);
                     validMapping.add(refer);
                  }
               } else {
                  if (null != ipaddress) {
                     logger.info("Notify Infoblox to query the assetname");
                     publisher.publish(null, ipaddress);
                  }
               }
            } else {
               validMapping.add(refer);
            }
         }
      }

      //Now we get all our valid mappings we need to extract the data for each exsi and push data.
      //         Map<SensorType, String> metricMapping = new HashMap<SensorType, String>();
      //         metricMapping.put(SensorType.BACKPANELTEMP, VROConsts.ENVRIONMENT_BACK_TEMPERATURE_METRIC);
      //         metricMapping.put(SensorType.FRONTPANELTEMP,
      //               VROConsts.ENVRIONMENT_FRONT_TEMPERATURE_METRIC);
      //         metricMapping.put(SensorType.PDU, VROConsts.ENVRIONMENT_POWER_METRIC);

      //         Set<String> validAssetIDs =
      //               validMapping.stream().map(ServerMapping::getAssetID).collect(Collectors.toSet());
      if (validMapping.isEmpty()) {
         logger.info("No Mapping find.Sync nothing for this execution.");
         return;
      }
      Long latencyFactor = latencyFactorMap.get(config.getServerURL());
      if (latencyFactor == null) {
         latencyFactor = 24L;
      }
      Long lastUpdateTimeStamp = lastUpdateTimeMap.get(config.getServerURL());

      if (lastUpdateTimeStamp == null) {
         lastUpdateTimeStamp = currentTime - FIVE_MINUTES * latencyFactor;
      }
      boolean hasNewData = false;
      logger.info(String.format("Start prepare data.%s, lastUpdateTime:%s, latencyFactor:%s",
            executionCount, lastUpdateTimeStamp, latencyFactor));
      long newUpdateTimeStamp = lastUpdateTimeStamp;
      for (ServerMapping mapping : validMapping) {
         if (mapping.getAsset() != null) {
            MetricData[] sensorDatas =
                  restClient.getServerRelatedSensorDataByServerID(mapping.getAsset(),
                        lastUpdateTimeStamp, FIVE_MINUTES * latencyFactor).getBody();
            Asset server = restClient.getAssetByID(mapping.getAsset()).getBody();
            List<String> pdus = server.getPdus();

            StatContents contents = new StatContents();
            StatContent frontTemp = new StatContent();
            List<Double> frontValues = new ArrayList<Double>();
            List<Long> frontTimes = new ArrayList<Long>();
            StatContent backTemp = new StatContent();
            List<Double> backValues = new ArrayList<Double>();
            List<Long> backTimes = new ArrayList<Long>();
            StatContent pduAMPSValue = new StatContent();
            List<Double> pduAMPSValues = new ArrayList<Double>();
            List<Long> pduAMPSTimes = new ArrayList<Long>();
            StatContent pduRealtimeVoltage = new StatContent();
            List<Double> voltageValues = new ArrayList<Double>();
            List<Long> voltageTimes = new ArrayList<Long>();
            StatContent pduRealtimePower = new StatContent();
            List<Double> powerValues = new ArrayList<Double>();
            List<Long> powerTimes = new ArrayList<Long>();
            StatContent frontHumidityPercent = new StatContent();
            List<Double> frontHumidityValues = new ArrayList<Double>();
            List<Long> frontHumidityTimes = new ArrayList<Long>();
            StatContent backHumidityPercent = new StatContent();
            List<Double> backHumidityValues = new ArrayList<Double>();
            List<Long> backHumidityTimes = new ArrayList<Long>();
            StatContent currentLoad = new StatContent();
            List<Double> currentLoadValues = new ArrayList<Double>();
            List<Long> currentLoadTimes = new ArrayList<Long>();
            StatContent powerLoad = new StatContent();
            List<Double> powerLoadValues = new ArrayList<Double>();
            List<Long> powerLoadTimes = new ArrayList<Long>();

            String pduId = null;
            String currentMetricName = MetricName.SERVER_CONNECTED_PDU_CURRENT;
            String powerMetricName = MetricName.SERVER_CONNECTED_PDU_POWER;
            String voltageMetricName = MetricName.SERVER_VOLTAGE;
            String currentLoadMetricName = MetricName.PDU_CURRENT_LOAD;
            String powerLoadMetricName = MetricName.PDU_POWER_LOAD;
            if(pdus!= null && !pdus.isEmpty()) {
               pduId = pdus.get(0);
               currentMetricName = String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_CURRENT, pduId);
               powerMetricName = String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_POWER, pduId);
               currentLoadMetricName = String.format(MetricKeyName.SERVER_CONNECTED_PDUX_CURRENT_LOAD, pduId);
               powerLoadMetricName = String.format(MetricKeyName.SERVER_CONNECTED_PDUX_POWER_LOAD, pduId);
            }

            for (MetricData data : sensorDatas) {
               if (data.getTimeStamp() > newUpdateTimeStamp) {
                  newUpdateTimeStamp = data.getTimeStamp();
                  hasNewData = true;
               }
               String metricName = data.getMetricName();
               if(metricName.equals(currentMetricName)) {
                  pduAMPSValues.add(data.getValueNum());
                  pduAMPSTimes.add(data.getTimeStamp());
                  continue;
               }else if(metricName.equals(powerMetricName)) {
                  powerValues.add(data.getValueNum());
                  powerTimes.add(data.getTimeStamp());
                  continue;
               }else if(metricName.equals(voltageMetricName)) {
                  voltageValues.add(data.getValueNum());
                  voltageTimes.add(data.getTimeStamp());
                  continue;
               }else if(metricName.equals(currentLoadMetricName)) {
                  currentLoadValues.add(data.getValueNum());
                  currentLoadTimes.add(data.getTimeStamp());
                  continue;
               }else if(metricName.equals(powerLoadMetricName)) {
                  powerLoadValues.add(data.getValueNum());
                  powerLoadTimes.add(data.getTimeStamp());
                  continue;
               }else if(metricName.contains(MetricName.SERVER_FRONT_HUMIDITY)) {
                  frontHumidityValues.add(data.getValueNum());
                  frontHumidityTimes.add(data.getTimeStamp());
                  continue;
               }else if(metricName.contains(MetricName.SERVER_FRONT_TEMPERATURE)) {
                  frontValues.add(data.getValueNum());
                  frontTimes.add(data.getTimeStamp());
                  continue;
               }else if(metricName.contains(MetricName.SERVER_BACK_TEMPREATURE)) {
                  backValues.add(data.getValueNum());
                  backTimes.add(data.getTimeStamp());
                  continue;
               }else if(metricName.contains(MetricName.SERVER_BACK_HUMIDITY)) {
                  backHumidityValues.add(data.getValueNum());
                  backHumidityTimes.add(data.getTimeStamp());
                  continue;
               }
            }
            if (!frontValues.isEmpty()) {
               frontTemp.setStatKey(VROConsts.ENVRIONMENT_FRONT_TEMPERATURE_METRIC);
               frontTemp.setData(frontValues.stream().mapToDouble(Double::valueOf).toArray());
               frontTemp.setTimestamps(frontTimes.stream().mapToLong(Long::valueOf).toArray());
               contents.addStatContent(frontTemp);
            }
            if (!backValues.isEmpty()) {
               backTemp.setStatKey(VROConsts.ENVRIONMENT_BACK_TEMPERATURE_METRIC);
               backTemp.setData(backValues.stream().mapToDouble(Double::valueOf).toArray());
               backTemp.setTimestamps(backTimes.stream().mapToLong(Long::valueOf).toArray());
               contents.addStatContent(backTemp);
            }
            if (!pduAMPSValues.isEmpty()) {
               pduAMPSValue.setStatKey(VROConsts.ENVRIONMENT_PDU_AMPS_METRIC);
               pduAMPSValue.setData(pduAMPSValues.stream().mapToDouble(Double::valueOf).toArray());
               pduAMPSValue.setTimestamps(pduAMPSTimes.stream().mapToLong(Long::valueOf).toArray());
               contents.addStatContent(pduAMPSValue);
            }
            if (!voltageValues.isEmpty()) {
               pduRealtimeVoltage.setStatKey(VROConsts.ENVRIONMENT_PDU_VOLTS_METRIC);
               pduRealtimeVoltage
                     .setData(voltageValues.stream().mapToDouble(Double::valueOf).toArray());
               pduRealtimeVoltage
                     .setTimestamps(voltageTimes.stream().mapToLong(Long::valueOf).toArray());
               contents.addStatContent(pduRealtimeVoltage);
            }
            if (!powerValues.isEmpty()) {
               pduRealtimePower.setStatKey(VROConsts.ENVRIONMENT_PDU_POWER_METRIC);
               pduRealtimePower
                     .setData(powerValues.stream().mapToDouble(Double::valueOf).toArray());
               pduRealtimePower
                     .setTimestamps(powerTimes.stream().mapToLong(Long::valueOf).toArray());
               contents.addStatContent(pduRealtimePower);
            }
            if (!frontHumidityValues.isEmpty()) {
               frontHumidityPercent.setStatKey(VROConsts.ENVRIONMENT_FRONT_HUMIDITY_METRIC);
               frontHumidityPercent
                     .setData(frontHumidityValues.stream().mapToDouble(Double::valueOf).toArray());
               frontHumidityPercent
                     .setTimestamps(frontHumidityTimes.stream().mapToLong(Long::valueOf).toArray());
               contents.addStatContent(frontHumidityPercent);
            }
            if (!backHumidityValues.isEmpty()) {
               backHumidityPercent.setStatKey(VROConsts.ENVRIONMENT_BACK_HUMIDITY_METRIC);
               backHumidityPercent
                     .setData(backHumidityValues.stream().mapToDouble(Double::valueOf).toArray());
               backHumidityPercent
                     .setTimestamps(backHumidityTimes.stream().mapToLong(Long::valueOf).toArray());
               contents.addStatContent(backHumidityPercent);
            }
            if (!currentLoadValues.isEmpty()) {
               currentLoad.setStatKey(VROConsts.ENVRIONMENT_PDU_AMPS_LOAD_METRIC);
               currentLoad
                     .setData(currentLoadValues.stream().mapToDouble(Double::valueOf).toArray());
               currentLoad
                     .setTimestamps(currentLoadTimes.stream().mapToLong(Long::valueOf).toArray());
               contents.addStatContent(currentLoad);
            }
            if (!powerLoadValues.isEmpty()) {
               powerLoad.setStatKey(VROConsts.ENVRIONMENT_PDU_POWER_LOAD_METRIC);
               powerLoad
                     .setData(powerLoadValues.stream().mapToDouble(Double::valueOf).toArray());
               powerLoad
                     .setTimestamps(powerLoadTimes.stream().mapToLong(Long::valueOf).toArray());
               contents.addStatContent(powerLoad);
            }

            if (!contents.getStatContents().isEmpty()) {
               logger.info("Push data to VRO: " + config.getServerURL());
               metricClient.addStats(null, UUID.fromString(mapping.getVroResourceID()), contents,
                     false);
            }
            //UPDATE THE PROPERTIES
            if (executionCount % 1000 == 0) {
               PropertyContents propertyContents = new PropertyContents();
               packagingPropertyContent(assetDictionary.get(mapping.getAsset()), propertyContents);
               metricClient.addProperties(null, UUID.fromString(mapping.getVroResourceID()),
                     propertyContents);
            }
         }
      }
      if (hasNewData) {
         Long factor = (currentTime - newUpdateTimeStamp) / FIVE_MINUTES + 1;
         if (factor > 24) {
            factor = 24L;
         } else if (factor < 0) {
            factor = 1L;
         }
         latencyFactorMap.put(config.getServerURL(), factor);
         if (newUpdateTimeStamp > currentTime) {
            newUpdateTimeStamp = currentTime;
         }
         lastUpdateTimeMap.put(config.getServerURL(), newUpdateTimeStamp);
      } else {
         latencyFactor = latencyFactor + 1;
         if (latencyFactor > 24) {
            latencyFactor = 24L;
         }
         latencyFactorMap.put(config.getServerURL(), latencyFactor);
      }
      executionCount++;
      logger.info("Finished Sync metric data for VRO: " + config.getServerURL());
   }

   private void packagingPropertyContent(Asset asset, PropertyContents contents) {
      long currenttime = System.currentTimeMillis();
      BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(asset);
      for (String key : methodPropertyMapping.keySet()) {
         PropertyContent content = new PropertyContent();
         content.setStatKey(methodPropertyMapping.get(key));
         content.setValues(new String[] { String.valueOf(wrapper.getPropertyValue(key)) });
         content.setTimestamps(new long[] { currenttime });
         contents.addPropertyContent(content);
      }

   }

   public void checkAndUpdateIntegrationStatus(SDDCSoftwareConfig vro, String message) {
      IntegrationStatus integrationStatus = vro.getIntegrationStatus();
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
         logger.error("Failed to sync data from VRO,error message is +" + message);
      }
      vro.setIntegrationStatus(integrationStatus);
      updateIntegrationStatus(vro);
   }

   private void syncVROMetricPropertyAlertDefinition(SDDCSoftwareConfig config) {
      if (!config.checkIsActive()) {
         return;
      }
      VROConfig vroConf = new VROConfig(config);
      MetricClient metricClient = null;
      logger.info("Synce predefinedMetrics for " + config.getServerURL());
      try {
         metricClient = new MetricClient(vroConf, publisher);
         metricClient.checkPredefinedMetricsandProperties();
      } catch (AuthException e) {
         logger.error("Failed to sync the metric data from VRO ", e);
         IntegrationStatus integrationStatus = config.getIntegrationStatus();
         if (integrationStatus == null) {
            integrationStatus = new IntegrationStatus();
         }
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(e.getMessage());
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         updateIntegrationStatus(config);
         return;
      } catch (Exception e) {
         if(e.getCause() instanceof UndeclaredThrowableException) {
            UndeclaredThrowableException undeclaredThrowableException = (UndeclaredThrowableException)e.getCause();
            if (undeclaredThrowableException.getUndeclaredThrowable().getCause() instanceof ConnectException) {
               checkAndUpdateIntegrationStatus(config,
                     undeclaredThrowableException.getUndeclaredThrowable().getCause().getMessage());
               return;
            }
         }
         throw e;
      }
      AlertClient alertClient = new AlertClient(vroConf);
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      alertClient.setRestClient(restClient);
      alertClient.run();
   }

}
