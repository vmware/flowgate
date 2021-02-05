/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.job;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.FlowgatePowerState;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ValueUnit.MetricUnit;
import com.vmware.flowgate.common.model.redis.message.AsyncService;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.EventUser;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.common.utils.WormholeDateFormat;
import com.vmware.flowgate.openmanage.client.OpenManageAPIClient;
import com.vmware.flowgate.openmanage.config.ServiceKeyConfig;
import com.vmware.flowgate.openmanage.datamodel.Chassis;
import com.vmware.flowgate.openmanage.datamodel.CommonResult;
import com.vmware.flowgate.openmanage.datamodel.Device;
import com.vmware.flowgate.openmanage.datamodel.DeviceMetric;
import com.vmware.flowgate.openmanage.datamodel.DeviceMetricsResult;
import com.vmware.flowgate.openmanage.datamodel.DevicePower;
import com.vmware.flowgate.openmanage.datamodel.DeviceStatus;
import com.vmware.flowgate.openmanage.datamodel.DeviceTemperature;
import com.vmware.flowgate.openmanage.datamodel.Duration;
import com.vmware.flowgate.openmanage.datamodel.EntityType;
import com.vmware.flowgate.openmanage.datamodel.MetricType;
import com.vmware.flowgate.openmanage.datamodel.Plugin;
import com.vmware.flowgate.openmanage.datamodel.PowerDisplayUnit;
import com.vmware.flowgate.openmanage.datamodel.PowerManageMetricsRequestBody;
import com.vmware.flowgate.openmanage.datamodel.PowerSetting;
import com.vmware.flowgate.openmanage.datamodel.PowerSettingType;
import com.vmware.flowgate.openmanage.datamodel.PowerState;
import com.vmware.flowgate.openmanage.datamodel.Server;
import com.vmware.flowgate.openmanage.datamodel.ServerSpecificData;
import com.vmware.flowgate.openmanage.datamodel.SortOrder;
import com.vmware.flowgate.openmanage.datamodel.TemperatureDisplayUnit;

@Service
public class OpenManageJobService implements AsyncService{

   private static final Logger logger = LoggerFactory.getLogger(OpenManageJobService.class);
   @Autowired
   private WormholeAPIClient wormholeApiClient;
   @Autowired
   private StringRedisTemplate template;
   @Autowired
   private ServiceKeyConfig serviceKeyConfig;
   @Value("${powermanager.enable:false}")
   private boolean powerManagerEnable;
   private static int defaultPageSize = 200;
   private static int defaultSkip = 0;
   public static final String OpenmanagePowerUnit = "watt";
   public static final String systemEnergyConsumptionUnit = "kilowatt-hour";
   public static final String temperatureUnit = "celsius";
   public static final String CIM = "CIM";
   public static final String PowerManager = "Power Manager";
   private ObjectMapper mapper = new ObjectMapper();
   private static Map<Integer,String> powerStateMap = new HashMap<Integer,String>();
   private static Map<String, MetricUnit> metricUnitMap = new HashMap<String, MetricUnit>();
   static {
      powerStateMap.put(PowerState.OFF.getValue(), FlowgatePowerState.OFFHARD.name());
      powerStateMap.put(PowerState.ON.getValue(), FlowgatePowerState.ON.name());
      powerStateMap.put(PowerState.POWERINGOFF.getValue(),FlowgatePowerState.OFFHARD.name());
      powerStateMap.put(PowerState.POWERINGON.getValue(), FlowgatePowerState.ON.name());
      powerStateMap.put(PowerState.UNKNOWN.getValue(), FlowgatePowerState.UNKNOWN.name());
      powerStateMap = Collections.unmodifiableMap(powerStateMap);
      metricUnitMap.put(OpenmanagePowerUnit, MetricUnit.W);
      metricUnitMap.put(systemEnergyConsumptionUnit, MetricUnit.KWH);
      metricUnitMap.put(temperatureUnit, MetricUnit.C);
      metricUnitMap.put(PowerSettingType.PowerUnit.name() + PowerDisplayUnit.Watt.getValue(),
            MetricUnit.W);
      metricUnitMap.put(PowerSettingType.PowerUnit.name() + PowerDisplayUnit.BTUPerHr.getValue(),
            MetricUnit.BTUPerHr);
      metricUnitMap.put(
            PowerSettingType.TemperatureUnit.name() + TemperatureDisplayUnit.Celsius.getValue(),
            MetricUnit.C);
      metricUnitMap.put(
            PowerSettingType.TemperatureUnit.name() + TemperatureDisplayUnit.Fahrenheit.getValue(),
            MetricUnit.F);
      metricUnitMap = Collections.unmodifiableMap(metricUnitMap);
   }

   @Override
   public void executeAsync(EventMessage message) {
      if (message.getType() != EventType.OpenManage) {
         logger.warn("Drop non-OpenManage message " + message.getType());
         return;
      }
      logger.info("message received");
      String messageString = null;
      while ((messageString = template.opsForList().rightPop(EventMessageUtil.OpenManageJobList)) != null) {
         EventMessage payloadMessage = null;
         try {
            payloadMessage = mapper.readValue(messageString, EventMessageImpl.class);
         } catch (IOException e) {
            logger.error("Cannot process message", e);
         }
         if (payloadMessage == null) {
            continue;
         }
         FacilitySoftwareConfig integration = null;
         try {
            integration =
                  mapper.readValue(payloadMessage.getContent(), FacilitySoftwareConfig.class);
         } catch (IOException e) {
            logger.error("Cannot process message", e);
         }
         if (null == integration) {
            continue;
         }
         if (!integration.checkIsActive()) {
            continue;
         }
         for (EventUser payloadCommand : payloadMessage.getTarget().getUsers()) {
            executeJob(payloadCommand.getId(), integration);
         }
      }
   }

   public void executeJob(String commanId,FacilitySoftwareConfig integration) {
      if(!integration.checkIsActive()) {
         return;
      }
      switch (commanId) {
      case EventMessageUtil.OpenManage_SyncAssetsMetaData:
         logger.info("Sync metadata for:"+ integration.getName());
         syncMetadataJob(integration);
         logger.info("Finished sync metadata job for:"+ integration.getName());
         break;
      case EventMessageUtil.OpenManage_SyncRealtimeData:
         logger.info("Sync metrics data for " + integration.getName());
         syncMetricsDataJob(integration);
         logger.info("Finished sync metrics data job for:"+ integration.getName());
         break;
      default:
         logger.warn("Unknown command");
         break;
      }
   }

   private void syncMetadataJob(FacilitySoftwareConfig integration) {
      wormholeApiClient.setServiceKey(serviceKeyConfig.getServiceKey());
      //check the status of integration
      try (OpenManageAPIClient client = createClient(integration);){
         checkConnection(client,integration);
         //sync server
         List<Asset> oldServers = wormholeApiClient.getAllAssetsBySourceAndType(integration.getId(), AssetCategory.Server);
         Map<Long,Asset> assetNumberMap = generateAssetNumberMap(oldServers);
         int skip = defaultSkip;
         while(true) {
            CommonResult<Server> serversResult = client.getDevices(skip, defaultPageSize, Server.class);
            int totalCount = serversResult.getCount();
            if(totalCount == defaultSkip) {
               logger.info("Not found server from : {}.", integration.getName());
               break;
            }
            if(!serversResult.getValue().isEmpty()) {
               List<Asset> serverAssetsToSave = handleServerAssets(serversResult, assetNumberMap, integration.getId());
               if(!serverAssetsToSave.isEmpty()) {
                  List<Asset> openmanageServerToUpdate = new ArrayList<Asset>();
                  for(Asset asset : serverAssetsToSave) {
                     ResponseEntity<Void> res = wormholeApiClient.saveAssets(asset);
                     if(res.getStatusCode().is2xxSuccessful()) {
                        String uriPath = res.getHeaders().getLocation().getPath();
                        String id = uriPath.substring(uriPath.lastIndexOf("/") + 1);
                        asset.setId(id);
                        createMetricFormulas(asset);
                        openmanageServerToUpdate.add(asset);
                     }
                  }
                  //update asset about metricsFormula
                  wormholeApiClient.saveAssets(openmanageServerToUpdate);
               }
            }
            skip += defaultPageSize;
            if(skip > totalCount) {
               break;
            }
         }

         //sync chassis
         List<Asset> oldChassis = wormholeApiClient.getAllAssetsBySourceAndType(integration.getId(), AssetCategory.Chassis);
         Map<Long,Asset> chassisAssetNumberMap = generateAssetNumberMap(oldChassis);
         int chassiItemSkip = defaultSkip;
         while(true) {
            CommonResult<Chassis> chassisResult = client.getDevices(chassiItemSkip, defaultPageSize, Chassis.class);
            int totalCount = chassisResult.getCount();
            if(totalCount == defaultSkip) {
               logger.info("Not found chassis from : {}.", integration.getName());
               break;
            }
            if(!chassisResult.getValue().isEmpty()) {
               List<Asset> chassiAssetsToSave = handleChassisAssets(chassisResult, chassisAssetNumberMap, integration.getId());
               if(!chassiAssetsToSave.isEmpty()) {
                  wormholeApiClient.saveAssets(chassiAssetsToSave);
               }
            }
            chassiItemSkip += defaultPageSize;
            if(chassiItemSkip > totalCount) {
               break;
            }
         }
      }
   }

   private void syncMetricsDataJob(FacilitySoftwareConfig integration) {
      wormholeApiClient.setServiceKey(serviceKeyConfig.getServiceKey());
      //check the status of integration
      try(OpenManageAPIClient client = createClient(integration);){
         checkConnection(client,integration);
         List<RealTimeData> metricData = getMetricDatas(integration, client);
         if (metricData.isEmpty()) {
            logger.info("Not found any metrics data from " + integration.getName());
            return;
         }
         wormholeApiClient.saveRealTimeData(metricData);
      }
   }

   /**
    *
    *Example of Temperature
     "@odata.context": "/api/$metadata#DeviceService.TemperatureResponseModel",
     "@odata.type": "#DeviceService.TemperatureResponseModel",
     "@odata.id": "/api/DeviceService/Devices(10074)/Temperature",
     "peakTemperatureUnit": "celsius",
     "avgTemperatureUnit": "celsius",
     "DateFormat": "CIM",
     "instantaneousTemperatureUnit": "celsius",
     "startTime": "20210201050207.000000-360",
     "avgTemperatureTimeStamp": "20210202093523.806515-360",
     "avgTemperature": "21",
     "instantaneousTemperature": "23",
     "peakTemperature": "27",
     "peakTemperatureTimeStamp": "20210201050207.000000-360"
    * @param integration
    * @param client
    * @return
    */
   public List<RealTimeData> getMetricDatas(FacilitySoftwareConfig integration,
         OpenManageAPIClient client){
      List<RealTimeData> metricDatas = new ArrayList<RealTimeData>();
      Map<String, String> assetIdAndOpenmanageDeviceIdMap = getMappedAssetIDMap(integration.getId());
      if(assetIdAndOpenmanageDeviceIdMap.isEmpty()) {
         return metricDatas;
      }
      Plugin powerManage = null;
      CommonResult<Plugin> plugins = client.getCommonResult(Plugin.class);
      for(Plugin plugin:plugins.getValue()) {
         if(PowerManager.equals(plugin.getName()) && plugin.isInstalled() && plugin.isEnabled()) {
            powerManage = plugin;
            break;
         }
      }
      for(Map.Entry<String, String> map:assetIdAndOpenmanageDeviceIdMap.entrySet()) {
         String deviceId = map.getValue();
         String assetId = map.getKey();
         Map<String, ValueUnit> metricNameAndValueUnitMap = getMetricsData(deviceId, client, powerManage);
         RealTimeData metricData = null;
         if(!metricNameAndValueUnitMap.isEmpty()) {
            metricData = new RealTimeData();
            List<ValueUnit> valueUnits = new ArrayList<ValueUnit>(metricNameAndValueUnitMap.values());
            long currentTime = valueUnits.get(0).getTime();
            metricData.setAssetID(assetId);
            metricData.setTime(currentTime);
            metricData.setValues(valueUnits);
            metricData.setId(assetId+currentTime);
            metricDatas.add(metricData);
         }
      }
      return metricDatas;
   }

   public List<Asset> handleServerAssets(CommonResult<Server> serversResult,
         Map<Long,Asset> assetNumberMap, String integrationId){
      List<Server> servers = serversResult.getValue();
      List<Asset> serverAssetsToSave = new ArrayList<Asset>();
      for(Server server:servers) {
         //filter by device status
         if(server.getStatus() == DeviceStatus.UNKNOWN.getValue()
               || server.getStatus() == DeviceStatus.NOSTATUS.getValue()) {
            continue;
         }
         Asset needToSaveServerAsset = null;
         if (assetNumberMap.containsKey(server.getId())) {
            needToSaveServerAsset = assetNumberMap.get(server.getId());
            if (deviceValueChanged(needToSaveServerAsset, server)) {
               serverAssetsToSave.add(needToSaveServerAsset);
            }
         }else {
            //new server asset
            needToSaveServerAsset = translateToAsset(server);
            needToSaveServerAsset.setCategory(AssetCategory.Server);
            needToSaveServerAsset.setAssetSource(integrationId);
            if(server.getDeviceSpecificData() != null) {
               ServerSpecificData data = server.getDeviceSpecificData();
               needToSaveServerAsset.setManufacturer(data.getManufacturer());
            }
            serverAssetsToSave.add(needToSaveServerAsset);
         }

      }
      return serverAssetsToSave;
   }

   public List<Asset> handleChassisAssets(CommonResult<Chassis> chassisResult,
         Map<Long,Asset> assetNumberMap, String integrationId){
      List<Chassis> chassisList = chassisResult.getValue();
      List<Asset> chassiAssetsToSave = new ArrayList<Asset>();
      for(Chassis chassis:chassisList) {
         //filter by device status
         if(chassis.getStatus() == DeviceStatus.UNKNOWN.getValue()
               || chassis.getStatus() == DeviceStatus.NOSTATUS.getValue()) {
            continue;
         }
         Asset needToSaveChassisAsset = null;
         if (assetNumberMap.containsKey(chassis.getId())) {
            needToSaveChassisAsset = assetNumberMap.get(chassis.getId());
            if (deviceValueChanged(needToSaveChassisAsset, chassis)) {
               chassiAssetsToSave.add(needToSaveChassisAsset);
            }
         }else {
            //new chassis asset
            needToSaveChassisAsset = translateToAsset(chassis);
            needToSaveChassisAsset.setCategory(AssetCategory.Chassis);
            needToSaveChassisAsset.setAssetSource(integrationId);
            chassiAssetsToSave.add(needToSaveChassisAsset);
         }

      }
      return chassiAssetsToSave;
   }

   public OpenManageAPIClient createClient(FacilitySoftwareConfig integration) {
      return new OpenManageAPIClient(integration);
   }

   private void updateIntegrationStatus(FacilitySoftwareConfig integration) {
      wormholeApiClient.updateFacility(integration);
   }

   private void checkConnection(OpenManageAPIClient client, FacilitySoftwareConfig integration) {
      try {
         client.getToken();
      } catch (ResourceAccessException resourceAccessException) {
         if (resourceAccessException.getCause().getCause() instanceof ConnectException) {
            checkAndUpdateIntegrationStatus(integration, resourceAccessException.getMessage());
         }
         throw resourceAccessException;
      } catch (HttpClientErrorException httpClientException) {
         if(httpClientException.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            IntegrationStatus integrationStatus = integration.getIntegrationStatus();
            if (integrationStatus == null) {
               integrationStatus = new IntegrationStatus();
            }
            integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
            integrationStatus.setDetail(httpClientException.getMessage());
            integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
            updateIntegrationStatus(integration);
         }
         throw httpClientException;
      }
   }

   private void checkAndUpdateIntegrationStatus(FacilitySoftwareConfig integration,String message) {
      IntegrationStatus integrationStatus = integration.getIntegrationStatus();
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
         logger.error("Failed to query data from integration");
      }
      integration.setIntegrationStatus(integrationStatus);
      updateIntegrationStatus(integration);
   }

   private boolean deviceValueChanged(Asset asset, Device device) {
      boolean changed = false;
      if (!StringUtils.equals(asset.getAssetName(), device.getDeviceName())) {
         asset.setAssetName(device.getDeviceName());
         changed = true;
      }
      if (!StringUtils.equals(asset.getTag(), device.getAssetTag())) {
         asset.setTag(device.getAssetTag());
         changed = true;
      }
      HashMap<String, String> justficationfileds = asset.getJustificationfields();
      if (!StringUtils.equals(justficationfileds.get(FlowgateConstant.POWERSTATE),
            powerStateMap.get(device.getPowerState()))) {
         justficationfileds.put(FlowgateConstant.POWERSTATE, powerStateMap.get(device.getPowerState()));
         asset.setJustificationfields(justficationfileds);
         changed = true;
      }
      return changed;
   }

   private Asset createMetricFormulas(Asset asset) {
      Map<String, String> formulas = new HashMap<String, String>();
      Map<String,String> metricNameAndIdMap = new HashMap<String,String>();
      metricNameAndIdMap.put(MetricName.SERVER_TEMPERATURE, asset.getId());
      metricNameAndIdMap.put(MetricName.SERVER_PEAK_TEMPERATURE, asset.getId());
      metricNameAndIdMap.put(MetricName.SERVER_AVERAGE_TEMPERATURE, asset.getId());
      metricNameAndIdMap.put(MetricName.SERVER_ENERGY_CONSUMPTION, asset.getId());
      metricNameAndIdMap.put(MetricName.SERVER_TOTAL_POWER, asset.getId());
      metricNameAndIdMap.put(MetricName.SERVER_AVERAGE_USED_POWER, asset.getId());
      metricNameAndIdMap.put(MetricName.SERVER_PEAK_USED_POWER, asset.getId());
      metricNameAndIdMap.put(MetricName.SERVER_MINIMUM_USED_POWER, asset.getId());
      formulas.put(FlowgateConstant.HOST_METRICS, asset.metricsFormulaToString(metricNameAndIdMap));
      asset.setMetricsformulars(formulas);
      return asset;
   }


   private Asset translateToAsset(Device device) {
      Asset asset = new Asset();
      asset.setAssetName(device.getDeviceName());
      asset.setTag(device.getAssetTag());
      asset.setModel(device.getModel());
      asset.getAssetSource();
      Map<String, String> openManageMap = new HashMap<String, String>();
      openManageMap.put(FlowgateConstant.ASSETNUMBER, String.valueOf(device.getId()));
      HashMap<String,String> justficationfileds = new HashMap<String,String>();
      try {
         String openManageInfo = mapper.writeValueAsString(openManageMap);
         justficationfileds.put(FlowgateConstant.OPENMANAGE, openManageInfo);
      } catch (JsonProcessingException e) {
         logger.error("Serializing device info map error", e);
      }
      if(powerStateMap.containsKey(device.getPowerState())) {
         justficationfileds.put(FlowgateConstant.POWERSTATE, powerStateMap.get(device.getPowerState()));
         asset.setJustificationfields(justficationfileds);
      }
      return asset;
   }

   private Map<Long, Asset> generateAssetNumberMap(List<Asset> assets) {
      Map<Long, Asset> assetNumberMap = new HashMap<Long, Asset>();
      for (Asset asset : assets) {
         HashMap<String, String> justficationfileds = asset.getJustificationfields();
         if (justficationfileds != null
               && justficationfileds.containsKey(FlowgateConstant.OPENMANAGE)) {
            String openManageInfo = justficationfileds.get(FlowgateConstant.OPENMANAGE);
            try {
               Map<String, String> infoMap = getInfoMap(openManageInfo);
               String openManageDeviceId = infoMap.get(FlowgateConstant.ASSETNUMBER);
               assetNumberMap.put(Long.valueOf(openManageDeviceId), asset);
            } catch (IOException ioException) {
               logger.error("Deserializing device info error", ioException);
               continue;
            }
         }
      }
      return assetNumberMap;
   }

   private Map<String, String> getInfoMap(String info) throws IOException {
      Map<String, String> infoMap = new HashMap<String, String>();
      if (info != null) {
         infoMap = mapper.readValue(info, new TypeReference<Map<String, String>>() {});
      }
      return infoMap;
   }

   private Map<String, String> getMappedAssetIDMap(String integrationId){
      Map<String, String> assetIdAndOpenmanageDeviceIdMap = new HashMap<String, String>();
      List<Asset> allMappedServer =
            Arrays.asList(wormholeApiClient.getMappedAsset(AssetCategory.Server).getBody());
      if (allMappedServer != null && !allMappedServer.isEmpty()) {
         for(Asset asset:allMappedServer) {
            if (!asset.getAssetSource().contains((integrationId))) {
               continue;
            }
            HashMap<String, String> justficationfileds = asset.getJustificationfields();
            if (justficationfileds != null
                  && justficationfileds.containsKey(FlowgateConstant.OPENMANAGE)) {
               String openManageInfo = justficationfileds.get(FlowgateConstant.OPENMANAGE);
               try {
                  Map<String, String> infoMap = getInfoMap(openManageInfo);
                  String openManageDeviceId = infoMap.get(FlowgateConstant.ASSETNUMBER);
                  assetIdAndOpenmanageDeviceIdMap.put(asset.getId(), openManageDeviceId);
               } catch (IOException ioException) {
                  logger.error("Deserializing device info error", ioException);
                  continue;
               }
            }
         }
      }
      return assetIdAndOpenmanageDeviceIdMap;
   }

   private Map<String, ValueUnit> getMetricsData(String deviceId, OpenManageAPIClient client, Plugin powerManage){
      DevicePower powerMetricsData = client.getDevicePowerMetrics(deviceId);
      long currentTime = 0;
      Map<String, ValueUnit> metricNameAndValueUnitMap = new HashMap<String, ValueUnit>();
      //Power metrics data from the Openmanage base API
      if(powerMetricsData != null && CIM.equals(powerMetricsData.getDateFormat())) {
         ValueUnit instantPower = new ValueUnit();
         currentTime = WormholeDateFormat.cimDateToMilliseconds(powerMetricsData.getInstantaneousHeadroomTimeStamp());
         instantPower.setKey(MetricName.SERVER_POWER);
         instantPower.setTime(currentTime);
         instantPower.setUnit(MetricUnit.KW.name());
         instantPower.setValueNum(Double.parseDouble(instantPower.translateUnit(String.valueOf(powerMetricsData.getPower()),
                        metricUnitMap.get(powerMetricsData.getPowerUnit()), MetricUnit.KW)));
         metricNameAndValueUnitMap.put(MetricName.SERVER_POWER, instantPower);

         long sinceTime = WormholeDateFormat.cimDateToMilliseconds(powerMetricsData.getSince());
         long systemEnergyConsumptionTime = WormholeDateFormat.cimDateToMilliseconds(powerMetricsData.getSystemEnergyConsumptionTimeStamp());
         ValueUnit systemEnergyConsumption = new ValueUnit();
         systemEnergyConsumption.setTime(systemEnergyConsumptionTime);
         systemEnergyConsumption.setExtraidentifier(String.valueOf(sinceTime));
         systemEnergyConsumption.setKey(MetricName.SERVER_ENERGY_CONSUMPTION);
         systemEnergyConsumption.setUnit(MetricUnit.KWH.name());
         systemEnergyConsumption.setValueNum(Double.parseDouble(systemEnergyConsumption.translateUnit(String.valueOf(powerMetricsData.getSystemEnergyConsumption()),
               metricUnitMap.get(powerMetricsData.getSystemEnergyConsumptionUnit()), MetricUnit.KWH)));
         metricNameAndValueUnitMap.put(MetricName.SERVER_ENERGY_CONSUMPTION, systemEnergyConsumption);

         ValueUnit avgPower = new ValueUnit();
         avgPower.setTime(currentTime);
         avgPower.setExtraidentifier(String.valueOf(sinceTime));
         avgPower.setKey(MetricName.SERVER_AVERAGE_USED_POWER);
         avgPower.setUnit(MetricUnit.KW.name());
         avgPower.setValueNum(Double.parseDouble(avgPower.translateUnit(String.valueOf(powerMetricsData.getAvgPower()),
               metricUnitMap.get(powerMetricsData.getAvgPowerUnit()), MetricUnit.KW)));
         metricNameAndValueUnitMap.put(MetricName.SERVER_AVERAGE_USED_POWER, avgPower);

         ValueUnit peakPower = new ValueUnit();
         peakPower.setTime(currentTime);
         long peakPowerTime = WormholeDateFormat.cimDateToMilliseconds(powerMetricsData.getPeakPowerTimeStamp());
         String sinceAndPeakTime = String.valueOf(sinceTime) + FlowgateConstant.SEPARATOR + String.valueOf(peakPowerTime);
         //Record the since time and peak power time, for example  1612417403074_FIELDSPLIT_1612415606985
         peakPower.setExtraidentifier(sinceAndPeakTime);
         peakPower.setKey(MetricName.SERVER_PEAK_USED_POWER);
         peakPower.setUnit(MetricUnit.KW.name());
         peakPower.setValueNum(Double.parseDouble(peakPower.translateUnit(String.valueOf(powerMetricsData.getPeakPower()),
               metricUnitMap.get(powerMetricsData.getPeakPowerUnit()), MetricUnit.KW)));
         metricNameAndValueUnitMap.put(MetricName.SERVER_PEAK_USED_POWER, peakPower);

         ValueUnit minimumPower = new ValueUnit();
         minimumPower.setTime(currentTime);
         long minimumTime = WormholeDateFormat.cimDateToMilliseconds(powerMetricsData.getMinimumPowerTimeStamp());
         String sinceAndMinimum = String.valueOf(sinceTime) + FlowgateConstant.SEPARATOR + String.valueOf(minimumTime);
         minimumPower.setExtraidentifier(sinceAndMinimum);
         minimumPower.setKey(MetricName.SERVER_MINIMUM_USED_POWER);
         minimumPower.setUnit(MetricUnit.KW.name());
         minimumPower.setValueNum(Double.parseDouble(minimumPower.translateUnit(String.valueOf(powerMetricsData.getMinimumPower()),
               metricUnitMap.get(powerMetricsData.getPeakPowerUnit()), MetricUnit.KW)));
         metricNameAndValueUnitMap.put(MetricName.SERVER_MINIMUM_USED_POWER, minimumPower);
      }
      //Temperature metrics data from the Openmanage base API
      DeviceTemperature temperatureMetrics = client.getDeviceTemperatureMetrics(deviceId);
      if(temperatureMetrics != null && CIM.equals(temperatureMetrics.getDateFormat())) {
         ValueUnit temperature = new ValueUnit();
         temperature.setKey(MetricName.SERVER_TEMPERATURE);
         temperature.setTime(currentTime);
         temperature.setUnit(MetricUnit.C.name());
         temperature.setValueNum(Double.parseDouble(temperature.translateUnit(String.valueOf(temperatureMetrics.getInstantaneousTemperature()),
               metricUnitMap.get(temperatureMetrics.getInstantaneousTemperatureUnit()), MetricUnit.C)));
         metricNameAndValueUnitMap.put(MetricName.SERVER_TEMPERATURE, temperature);

         long startTime = WormholeDateFormat.cimDateToMilliseconds(temperatureMetrics.getStartTime());
         ValueUnit avgTemperature = new ValueUnit();
         avgTemperature.setKey(MetricName.SERVER_AVERAGE_TEMPERATURE);
         avgTemperature.setExtraidentifier(String.valueOf(startTime));
         avgTemperature.setTime(currentTime);
         avgTemperature.setUnit(MetricUnit.C.name());
         avgTemperature.setValueNum(Double.parseDouble(avgTemperature.translateUnit(String.valueOf(temperatureMetrics.getAvgTemperature()),
               metricUnitMap.get(temperatureMetrics.getAvgTemperatureUnit()), MetricUnit.C)));
         metricNameAndValueUnitMap.put(MetricName.SERVER_AVERAGE_TEMPERATURE, avgTemperature);

         ValueUnit peakTemperature = new ValueUnit();
         peakTemperature.setKey(MetricName.SERVER_PEAK_TEMPERATURE);
         long peakTime = WormholeDateFormat.cimDateToMilliseconds(temperatureMetrics.getPeakTemperatureTimeStamp());
         String startAndPeakTime = String.valueOf(startTime) + FlowgateConstant.SEPARATOR + String.valueOf(peakTime);
         peakTemperature.setExtraidentifier(startAndPeakTime);
         peakTemperature.setTime(currentTime);
         peakTemperature.setUnit(MetricUnit.C.name());
         peakTemperature.setValueNum(Double.parseDouble(peakTemperature.translateUnit(String.valueOf(temperatureMetrics.getPeakTemperature()),
               metricUnitMap.get(temperatureMetrics.getPeakTemperatureUnit()), MetricUnit.C)));
         metricNameAndValueUnitMap.put(MetricName.SERVER_PEAK_TEMPERATURE, peakTemperature);
      }
      //get metrics form power Manage plugin
      if(powerManage != null && powerManagerEnable) {
         String pluginId = powerManage.getId();
         PowerManageMetricsRequestBody body = new PowerManageMetricsRequestBody();
         body.setPluginId(pluginId);
         body.setEntityType(EntityType.Device.getValue());
         body.setEntityId(Integer.valueOf(deviceId));
         List<Integer> metricTypes = new ArrayList<Integer>();
         metricTypes.add(MetricType.INSTANT_POWER.getValue());
         metricTypes.add(MetricType.INSTANT_TEMP.getValue());//Inlet temperature
         body.setMetricTypes(metricTypes);
         body.setDuration(Duration.Recent.getValue());
         body.setSortOrder(SortOrder.Ascending.getValue());
         DeviceMetricsResult metrics = null;
         try {
            metrics = client.getMetricsFromPowerManage(body);
         }catch (HttpClientErrorException e) {
            logger.error("Not found any metrics data.", e);
            return metricNameAndValueUnitMap;
         }
         List<DeviceMetric> metricValues = metrics.getValue();
         if(metricValues.isEmpty()) {
            return metricNameAndValueUnitMap;
         }
         Map<Integer, String> metricTypeMap = new HashMap<Integer, String>();
         metricTypeMap.put(MetricType.INSTANT_POWER.getValue(), MetricName.SERVER_TOTAL_POWER);
         metricTypeMap.put(MetricType.INSTANT_TEMP.getValue(), MetricName.SERVER_FRONT_TEMPERATURE);
         CommonResult<PowerSetting> powerSettings = client.getCommonResult(PowerSetting.class);
         Map<Integer, Integer> powerSettingTypeAndValueMap = new HashMap<Integer, Integer>();
         for(PowerSetting powerSetting:powerSettings.getValue()) {
            powerSettingTypeAndValueMap.put(powerSetting.getId(), powerSetting.getValue());
         }
         for(DeviceMetric metric : metricValues) {
            if (MetricType.INSTANT_POWER.getValue() == metric.getType()) {
               ValueUnit instantPower = new ValueUnit();
               instantPower.setKey(MetricName.SERVER_POWER);
               instantPower.setTime(currentTime);
               instantPower.setUnit(MetricUnit.KW.name());
               int unitValue =
                     powerSettingTypeAndValueMap.get(PowerSettingType.PowerUnit.getValue());
               //PowerUnit1 : watt
               //PowerUnit2 : BtuPerHr
               MetricUnit powerSourceUnit =
                     metricUnitMap.get(PowerSettingType.PowerUnit.name() + unitValue);
               instantPower.setValueNum(Double.valueOf(instantPower.translateUnit(
                     String.valueOf(metric.getValue()), powerSourceUnit, MetricUnit.KW)));
               metricNameAndValueUnitMap.put(MetricName.SERVER_POWER, instantPower);

            }else if (MetricType.INSTANT_TEMP.getValue() == metric.getType()) {
               ValueUnit inletTemperature = new ValueUnit();
               inletTemperature.setKey(MetricName.SERVER_FRONT_TEMPERATURE);
               inletTemperature.setTime(currentTime);
               inletTemperature.setUnit(MetricUnit.C.name());
               int unitValue =
                     powerSettingTypeAndValueMap.get(PowerSettingType.TemperatureUnit.getValue());
               MetricUnit temperatureSourceUnit =
                     metricUnitMap.get(PowerSettingType.TemperatureUnit.name() + unitValue);
               inletTemperature.setValueNum(Double.valueOf(inletTemperature.translateUnit(
                     String.valueOf(metric.getValue()), temperatureSourceUnit, MetricUnit.C)));
               metricNameAndValueUnitMap.put(MetricName.SERVER_FRONT_TEMPERATURE, inletTemperature);
            }
         }
      }
      return metricNameAndValueUnitMap;
   }
}
