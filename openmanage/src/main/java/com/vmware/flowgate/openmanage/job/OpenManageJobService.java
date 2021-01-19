/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.job;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
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
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.redis.message.AsyncService;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.EventUser;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.openmanage.client.OpenManageAPIClient;
import com.vmware.flowgate.openmanage.config.ServiceKeyConfig;
import com.vmware.flowgate.openmanage.datamodel.Chassis;
import com.vmware.flowgate.openmanage.datamodel.Device;
import com.vmware.flowgate.openmanage.datamodel.DeviceStatus;
import com.vmware.flowgate.openmanage.datamodel.DevicesResult;
import com.vmware.flowgate.openmanage.datamodel.PowerState;
import com.vmware.flowgate.openmanage.datamodel.Server;
import com.vmware.flowgate.openmanage.datamodel.ServerSpecificData;

@Service
public class OpenManageJobService implements AsyncService{

   private static final Logger logger = LoggerFactory.getLogger(OpenManageJobService.class);
   @Autowired
   private WormholeAPIClient wormholeApiClient;
   @Autowired
   private StringRedisTemplate template;
   @Autowired
   private ServiceKeyConfig serviceKeyConfig;
   private static int defaultPageSize = 200;
   private static int defaultSkip = 0;
   private ObjectMapper mapper = new ObjectMapper();
   private static Map<Integer,String> powerStateMap = new HashMap<Integer,String>();
   static {
      powerStateMap.put(PowerState.OFF.getValue(), FlowgatePowerState.OFFHARD.name());
      powerStateMap.put(PowerState.ON.getValue(), FlowgatePowerState.ON.name());
      powerStateMap.put(PowerState.POWERINGOFF.getValue(),FlowgatePowerState.OFFHARD.name());
      powerStateMap.put(PowerState.POWERINGON.getValue(), FlowgatePowerState.ON.name());
      powerStateMap.put(PowerState.UNKNOWN.getValue(), FlowgatePowerState.UNKNOWN.name());
      powerStateMap = Collections.unmodifiableMap(powerStateMap);
   }

   @Override
   public void executeAsync(EventMessage message) {
      if (message.getType() != EventType.OpenManage) {
         logger.warn("Drop non-OpenManage message " + message.getType());
         return;
      }
      logger.info("message received");
      Set<EventUser> users = message.getTarget().getUsers();
      for (EventUser command : users) {
         logger.info(command.getId());
         switch (command.getId()) {
         case EventMessageUtil.OpenManage_SyncData:
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
            break;
         default:
            FacilitySoftwareConfig openmanage = null;
            try {
               openmanage = mapper.readValue(message.getContent(), FacilitySoftwareConfig.class);
            } catch (IOException ioException) {
               logger.error("Failed to convert message", ioException);
            }
            if (openmanage != null && openmanage.checkIsActive()) {
               executeJob(command.getId(), openmanage);
            }
            break;
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
         logger.info("Finished sync metadata.");
         break;
      case EventMessageUtil.OpenManage_SyncRealtimeData:
         logger.info("Sync metrics data for " + integration.getName());
         syncMetricsDataJob(integration);
         logger.info("Finished sync metrics data." );
         break;
      default:
         logger.warn("Unknown command");
         break;
      }
   }

   private void syncMetadataJob(FacilitySoftwareConfig integration) {
      wormholeApiClient.setServiceKey(serviceKeyConfig.getServiceKey());
      //check the status of integration
      OpenManageAPIClient client = createClient(integration);
      try {
         checkConnection(client,integration);
         //sync server
         List<Asset> oldServers = wormholeApiClient.getAllAssetsBySourceAndType(integration.getId(), AssetCategory.Server);
         Map<Long,Asset> assetNumberMap = generateAssetNumberMap(oldServers);
         int skip = defaultSkip;
         while(true) {
            DevicesResult<Server> serversResult = client.getDevices(skip, defaultPageSize, Server.class);
            int totalCount = serversResult.getCount();
            if(totalCount == defaultSkip) {
               logger.info("Not found server device from : {}.", integration.getName());
               break;
            }
            if(!serversResult.getValue().isEmpty()) {
               List<Asset> serverAssetsToSave = handleServerAssets(serversResult, assetNumberMap);
               if(!serverAssetsToSave.isEmpty()) {
                  wormholeApiClient.saveAssets(serverAssetsToSave);
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
            DevicesResult<Chassis> chassisResult = client.getDevices(chassiItemSkip, defaultPageSize, Chassis.class);
            int totalCount = chassisResult.getCount();
            if(totalCount == defaultSkip) {
               logger.info("Not found chassis device from : {}.", integration.getName());
               break;
            }
            if(!chassisResult.getValue().isEmpty()) {
               List<Asset> chassiAssetsToSave = handleChassisAssets(chassisResult, chassisAssetNumberMap);
               if(!chassiAssetsToSave.isEmpty()) {
                  wormholeApiClient.saveAssets(chassiAssetsToSave);
               }
            }
            chassiItemSkip += defaultPageSize;
            if(chassiItemSkip > totalCount) {
               break;
            }
         }
      }finally {
         client.logOut();
      }
   }

   private void syncMetricsDataJob(FacilitySoftwareConfig integration) {
      wormholeApiClient.setServiceKey(serviceKeyConfig.getServiceKey());
      //check the status of integration
      OpenManageAPIClient client = createClient(integration);
      checkConnection(client,integration);
      //put your sync metrics data logic here
      /**
       *
         1.Get data from your system
            For example:
            AdapterClient client = createClient(integration);
            yourData = client.getDataFromCustomerApi();
         2.Translate your data model to the flowgate data model
         3.Save the data to flowgate, you need to check Flowgate API-Client in the common-restclient
            For example
            wormholeApiClient.saveAssets(asset);
       */

   }

   public List<Asset> handleServerAssets(DevicesResult<Server> serversResult, Map<Long,Asset> assetNumberMap ){
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
            continue;
         }
         //new server asset
         needToSaveServerAsset = translateToAsset(server);
         needToSaveServerAsset.setCategory(AssetCategory.Server);
         if(server.getDeviceSpecificData() != null) {
            ServerSpecificData data = server.getDeviceSpecificData();
            needToSaveServerAsset.setManufacturer(data.getManufacturer());
         }
         serverAssetsToSave.add(needToSaveServerAsset);
      }
      return serverAssetsToSave;
   }

   public List<Asset> handleChassisAssets(DevicesResult<Chassis> chassisResult, Map<Long,Asset> assetNumberMap ){
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
            continue;
         }
         //new chassis asset
         needToSaveChassisAsset = translateToAsset(chassis);
         needToSaveChassisAsset.setCategory(AssetCategory.Chassis);
         chassiAssetsToSave.add(needToSaveChassisAsset);
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

   private boolean valueIsChanged(String oldValue, String newValue) {
      String value = oldValue == null ? "" : oldValue;
      String comparedValue = newValue == null ? "" : newValue;
      return !value.equals(comparedValue);
   }

   private boolean deviceValueChanged(Asset asset, Device device) {
      boolean changed = false;
      if (valueIsChanged(asset.getAssetName(),
            device.getDeviceName())) {
         asset.setAssetName(device.getDeviceName());
         changed = true;
      }
      if (valueIsChanged(asset.getTag(), device.getAssetTag())) {
         asset.setTag(device.getAssetTag());
         changed = true;
      }
      HashMap<String, String> justficationfileds = asset.getJustificationfields();
      if (valueIsChanged(justficationfileds.get(FlowgateConstant.POWERSTATE),
            powerStateMap.get(device.getPowerState()))) {
         justficationfileds.put(FlowgateConstant.POWERSTATE,powerStateMap.get(device.getPowerState()));
         asset.setJustificationfields(justficationfileds);
         changed = true;
      }
      return changed;
   }

   private Asset translateToAsset(Device device) {
      Asset asset = new Asset();
      asset.setAssetName(device.getDeviceName());
      asset.setTag(device.getAssetTag());
      asset.setModel(device.getModel());
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
}
