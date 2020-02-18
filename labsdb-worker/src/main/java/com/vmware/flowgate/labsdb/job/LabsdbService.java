/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.labsdb.job;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetStatus;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.NetworkMapping;
import com.vmware.flowgate.common.PduMapping;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.redis.message.AsyncService;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.EventUser;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.labsdb.client.LabsdbClient;
import com.vmware.flowgate.labsdb.common.EndDevice;
import com.vmware.flowgate.labsdb.config.ServiceKeyConfig;
import com.vmware.flowgate.labsdb.util.WiremapSaxHandler;

@Service
public class LabsdbService implements AsyncService{

   private static final Logger logger = LoggerFactory.getLogger(LabsdbService.class);
   @Autowired
   private WormholeAPIClient wormholeApiClient;
   @Autowired
   private StringRedisTemplate template;
   @Autowired
   private ServiceKeyConfig serviceKeyConfig;
   private ObjectMapper mapper = new ObjectMapper();
   private static final String wirmMap_node = "PORT";

   @Override
   public void executeAsync(EventMessage message) {
      // TODO Auto-generated method stub
      if (message.getType() != EventType.Labsdb) {
         logger.warn("Drop none Labsdb message " + message.getType());
         return;
      }
      logger.info("message received");
      Set<EventUser> users = message.getTarget().getUsers();
      for (EventUser command : users) {
         logger.info(command.getId());
         switch (command.getId()) {
         case EventMessageUtil.Labsdb_SyncData:
            //it will sync all the data depend on the type in the labsdbJobList.
            String messageString = null;
            while ((messageString =
                  template.opsForList().rightPop(EventMessageUtil.labsdbJobList)) != null) {
               EventMessage payloadMessage = null;
               try {
                  payloadMessage = mapper.readValue(messageString, EventMessageImpl.class);
               } catch (IOException e) {
                  logger.error("Cannot process message", e);
               }
               if (payloadMessage == null) {
                  continue;
               }
               FacilitySoftwareConfig labsdb = null;
               try {
                  labsdb =
                        mapper.readValue(payloadMessage.getContent(), FacilitySoftwareConfig.class);
               } catch (IOException e) {
                  logger.error("Cannot process message", e);
               }
               if (null == labsdb) {
                  continue;
               }
               if(!labsdb.checkIsActive()) {
                  continue;
               }
               for (EventUser payloadCommand : payloadMessage.getTarget().getUsers()) {
                  excuteJob(payloadCommand.getId(),labsdb);
               }
            }
            break;
         default:
            FacilitySoftwareConfig labsdb = null;
            try {
               labsdb = mapper.readValue(message.getContent(), FacilitySoftwareConfig.class);
            } catch (IOException e1) {
               // TODO Auto-generated catch block
               logger.error("Failed to convert message", e1);
            }
            if (labsdb != null) {
               excuteJob(command.getId(),labsdb);
            }
            break;
         }
      }
   }

   public void excuteJob(String commanId,FacilitySoftwareConfig labsdb) {
      if(!labsdb.checkIsActive()) {
         return;
      }
      switch (commanId) {
      case EventMessageUtil.Labsdb_SyncAllWireMapData:
         logger.info("Full Sync for:"+ labsdb.getName());
         syncWiremapData(labsdb,true);
         logger.info("Full sync finished.");
         break;
      case EventMessageUtil.Labsdb_SyncUnMappedAssetWiremapData:
         logger.info("Incremental sync for " + labsdb.getName());
         syncWiremapData(labsdb,false);
         logger.info("Incremental sync finished." );
         break;
      default:
         logger.warn("Unknown command");
         break;
      }
   }

   private void updateIntegrationStatus(FacilitySoftwareConfig labsdb) {
      wormholeApiClient.setServiceKey(serviceKeyConfig.getServiceKey());
      wormholeApiClient.updateFacility(labsdb);
   }

   public void checkAndUpdateIntegrationStatus(FacilitySoftwareConfig labsdb,String message) {
      IntegrationStatus integrationStatus = labsdb.getIntegrationStatus();
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
         logger.error("Failed to query data from Labsdb");
      }
      labsdb.setIntegrationStatus(integrationStatus);
      updateIntegrationStatus(labsdb);
   }

   public void syncWiremapData(FacilitySoftwareConfig config,boolean isAll) {
      wormholeApiClient.setServiceKey(serviceKeyConfig.getServiceKey());
      List<Asset> servers = wormholeApiClient.getAllAssetsByType(AssetCategory.Server);
      if(!isAll) {
         servers = filterServers(servers);
      }
      Map<String,String> pduIDListMap = getAssetNameIDMap(AssetCategory.PDU);
      Map<String,String> networkIDListMap = getAssetNameIDMap(AssetCategory.Networks);
      LabsdbClient labsdbClient = createClient(config);
      try {
         if(!labsdbClient.checkConnection()) {
            return;
         }
      }catch(HttpClientErrorException e) {
         logger.error("Failed to query data from Labsdb", e);
         IntegrationStatus integrationStatus = config.getIntegrationStatus();
         if(integrationStatus == null) {
            integrationStatus = new IntegrationStatus();
         }
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(e.getMessage());
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         updateIntegrationStatus(config);
         return;
      }catch(ResourceAccessException e1) {
         if(e1.getCause().getCause() instanceof ConnectException) {
            checkAndUpdateIntegrationStatus(config, e1.getMessage());
            return;
         }
       }
      //generatorWiremapData(servers,pduIDListMap,networkIDListMap,labsdbClient);
      SAXParserFactory spf = SAXParserFactory.newInstance();
      WiremapSaxHandler handler = new WiremapSaxHandler(wirmMap_node);
      SAXParser parser = null;
      try {
         parser = spf.newSAXParser();
      } catch (ParserConfigurationException | SAXException e) {
         logger.error("Create new sax parser failed."+e.getMessage());
      }
      for(Asset asset:servers) {
         ResponseEntity<String> resultEntity = null;
         try {
            resultEntity = labsdbClient.getWireMap(asset.getAssetName());
         }catch(Exception e) {
            logger.error("An exception occurred while accessing the labsdb server."+e.getMessage());
         }
         if(resultEntity == null || resultEntity.getBody() == null) {
            continue;
         }
         try {
            parser.parse(new ByteArrayInputStream(resultEntity.getBody().getBytes()), handler);
         } catch(SAXException | IOException e) {
            logger.error("Error parsing XML input stream.This XML input stream is "+resultEntity.getBody());
         }
         List<EndDevice> devices = handler.getEndDevices();//Get all the devices connected to the server
         if(devices == null || devices.isEmpty()) {
            continue;
         }
         generatorWiremapData(asset,pduIDListMap,devices,networkIDListMap);
         wormholeApiClient.saveAssets(asset);
      }
   }

   public Asset generatorWiremapData(Asset asset,Map<String,String> pduNameAndIdMap,
         List<EndDevice> devices,Map<String,String> networkNameAndIdMap){
      Set<String> pduIDList = null;
      Set<String> networkIDList = null;
      pduIDList = new HashSet<String>();
      networkIDList = new HashSet<String>();
      HashMap<String, String> justficationfields = asset.getJustificationfields();
      String pduPortString = null;
      String networkPortString = null;
      Set<String> pduDevices = new HashSet<String>();
      Set<String> networkDevices = new HashSet<String>();
      if (justficationfields != null) {
         pduPortString = justficationfields.get(FlowgateConstant.PDU_PORT_FOR_SERVER);
         networkPortString = justficationfields.get(FlowgateConstant.NETWORK_PORT_FOR_SERVER);
      }
      if (pduPortString != null) {
         String pdus[] = pduPortString.split(FlowgateConstant.SPILIT_FLAG);
         Collections.addAll(pduDevices, pdus);
      }
      if (networkPortString != null) {
         String networks[] = networkPortString.split(FlowgateConstant.SPILIT_FLAG);
         Collections.addAll(networkDevices, networks);
      }
      //Use the device name to find it, and if it exists, record it's number and port information.
      for (EndDevice device : devices) {
         if (device.getStartPort() == null || device.getEndPort() == null
               || device.getEndDeviceName() == null) {
            continue;
         }
         String pduId = pduNameAndIdMap.get(device.getEndDeviceName());
         if (pduId != null) {
            pduIDList.add(pduId);
            device.setEndDeviceAssetId(pduId);
            device.setEndPort(FlowgateConstant.OUTLET_NAME_PREFIX+device.getEndPort());
            String pduDevice = device.toString();
            pduDevices.add(pduDevice);
         } else {
            String networkId = networkNameAndIdMap.get(device.getEndDeviceName());
            if (networkId != null) {
               networkIDList.add(networkId);
               device.setEndDeviceAssetId(networkId);
               String networkDevice = device.toString();
               networkDevices.add(networkDevice);
            } else {
               continue;
            }
         }
      }
      if (!pduDevices.isEmpty()) {
         pduPortString = String.join(FlowgateConstant.SPILIT_FLAG, pduDevices);
         justficationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, pduPortString);
      }
      if (!networkDevices.isEmpty()) {
         networkPortString = String.join(FlowgateConstant.SPILIT_FLAG, networkDevices);
         justficationfields.put(FlowgateConstant.NETWORK_PORT_FOR_SERVER, networkPortString);
      }
      asset.setJustificationfields(justficationfields);
      //update the mapping status,from UNPPED or MAPPEDBYAGGREGATOR to MAPPEDBYLABSDB
      AssetStatus status = asset.getStatus();
      if (status == null) {
         status = new AssetStatus();
      }
      if (!pduIDList.isEmpty()) {
         status.setPduMapping(PduMapping.MAPPEDBYLABSDB);
         asset.setPdus(new ArrayList<String>(pduIDList));
      }
      if (!networkIDList.isEmpty()) {
         status.setNetworkMapping(NetworkMapping.MAPPEDBYLABSDB);
         asset.setSwitches(new ArrayList<String>(networkIDList));
      }
      asset.setStatus(status);
      return asset;
   }

   LabsdbClient createClient(FacilitySoftwareConfig config) {
      return new LabsdbClient(config);
   }

   //The asset's status will be updated when the aggregator job excute.
   //eg:From UNMAPPED changed to MAPPEDBYAGGREGATOR
   public List<Asset> filterServers(List<Asset> servers){
      List<Asset> unMappedServer = new ArrayList<Asset>();
      for(Asset server:servers) {
         if(server.getStatus() == null || (server.getStatus().getPduMapping() == null &&  server.getStatus().getNetworkMapping() == null)) {
            unMappedServer.add(server);
         }else if(server.getStatus().getPduMapping().getWeight()<PduMapping.MAPPEDBYLABSDB.getWeight() ||
               server.getStatus().getNetworkMapping().getWeight()<NetworkMapping.MAPPEDBYLABSDB.getWeight()) {
            unMappedServer.add(server);
         }else {
            continue;
         }
      }
      return unMappedServer;
   }

   public Map<String,String> getAssetNameIDMap(AssetCategory category){
      Map<String,String> assetNameAndIdMap = new HashMap<String,String>();
      List<Asset> assets = wormholeApiClient.getAllAssetsByType(category);
      for(Asset asset:assets) {
         assetNameAndIdMap.put(asset.getAssetName(), asset.getId());
      }
      return assetNameAndIdMap;
   }

}
