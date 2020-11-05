/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.infobloxworker.jobs;

import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.redis.message.AsyncService;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.infobloxworker.config.ServiceKeyConfig;
import com.vmware.flowgate.infobloxworker.model.InfoBloxResult;
import com.vmware.flowgate.infobloxworker.service.InfobloxClient;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.util.List;

@Service
public class InfoBloxService implements AsyncService {

   private final static Logger logger = LoggerFactory.getLogger(InfoBloxService.class);
   @Autowired
   private WormholeAPIClient wormholeAPIClient;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;
   @Autowired
   private MessagePublisher publisher;

   @Value("${redis.infoblox.publisher:aggregator}")
   private String hostMappingTopic;


   @Override
   @Async("asyncServiceExecutor")
   public void executeAsync(EventMessage eventMessage) {

      //The data format of the message. should be an ip address.
      //Do the business here.
      String message = eventMessage.getContent();
      logger.info("Try to find hostname for ip: {}", message);
      //check message , make sure it is an valid ip address;
      wormholeAPIClient.setServiceKey(serviceKeyConfig.getServiceKey());
      FacilitySoftwareConfig[] infoBloxes = wormholeAPIClient.getFacilitySoftwareByType(SoftwareType.InfoBlox).getBody();
      AssetIPMapping[] mappings = wormholeAPIClient.getHostnameIPMappingByIP(message).getBody();
      for (FacilitySoftwareConfig infoblox : infoBloxes) {
         if(!infoblox.checkIsActive()) {
            continue;
         }
         InfobloxClient client = new InfobloxClient(infoblox);
         List<InfoBloxResult> infoBloxResults = null;
         IntegrationStatus integrationStatus = infoblox.getIntegrationStatus();
         try {
            infoBloxResults = client.queryHostNamesByIP(message);
         } catch (ResourceAccessException e) {
            if(e.getCause().getCause() instanceof ConnectException) {
               checkAndUpdateIntegrationStatus(infoblox, e.getCause().getCause().getMessage());
               return;
            }
         } catch (HttpClientErrorException e1) {
             logger.error("Failed to query data from Infoblox", e1);
             if(integrationStatus == null) {
                integrationStatus = new IntegrationStatus();
             }
             integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
             integrationStatus.setDetail(e1.getMessage());
             integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
             updateIntegrationStatus(infoblox);
             return;
          }

         if(integrationStatus.getRetryCounter() > 0) {
            integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
            updateIntegrationStatus(infoblox);
         }

         if (infoBloxResults != null && !infoBloxResults.isEmpty()) {
            for (InfoBloxResult infoBloxResult : infoBloxResults) {
               try {
                  ResponseEntity<Asset> asset = wormholeAPIClient.getAssetByName(infoBloxResult.getHostName());
                  if (asset == null || asset.getBody() == null) {
                     logger.info("hostname ({}) no found!", infoBloxResult.getHostName());
                     continue;
                  }
               } catch (HttpClientErrorException e) {
                  logger.error("Error when searching {}", infoBloxResult.getHostName(), e);
                  continue;
               }
               AssetIPMapping tempMapping = new AssetIPMapping();
               tempMapping.setAssetname(infoBloxResult.getHostName());
               tempMapping.setMacAddress(infoBloxResult.getMacAddress());
               tempMapping.setIp(message);

               boolean isNewMapping = true;
               if (null != mappings && mappings.length > 0) {
                  for (AssetIPMapping mapping : mappings) {
                     if (tempMapping.getAssetname().equals(mapping.getAssetname())
                              && (Strings.isBlank(tempMapping.getMacAddress()) || tempMapping.getMacAddress().equals(mapping.getMacAddress()))) {
                        isNewMapping = false;
                        break;
                     }
                  }
               }
               if (isNewMapping) {
                  wormholeAPIClient.createHostnameIPMapping(tempMapping);
               }
               logger.info("Find hostname {} for ip {}", infoBloxResult.getHostName(), message);
            }
         } else {
            logger.info("Cannot find the hostname for IP: {}", message);
         }
      }
   }

   private void checkAndUpdateIntegrationStatus(FacilitySoftwareConfig infoblox,String message) {
      IntegrationStatus integrationStatus = infoblox.getIntegrationStatus();
      if(integrationStatus == null) {
         integrationStatus = new IntegrationStatus();
      }
      int timesOftry = integrationStatus.getRetryCounter();
      timesOftry++;
      if(timesOftry < FlowgateConstant.MAXNUMBEROFRETRIES) {
         integrationStatus.setRetryCounter(timesOftry);
      }else {
         logger.error("Failed to query data from Infoblox");
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(message);
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
      }
      updateIntegrationStatus(infoblox);
   }

   private void updateIntegrationStatus(FacilitySoftwareConfig infoblox) {
      wormholeAPIClient.setServiceKey(serviceKeyConfig.getServiceKey());
      wormholeAPIClient.updateFacility(infoblox);
   }
}
