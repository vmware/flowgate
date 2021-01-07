/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.job;

import java.io.IOException;
import java.net.ConnectException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.redis.message.AsyncService;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventUser;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.openmanage.client.OpenManageAPIClient;
import com.vmware.flowgate.openmanage.config.ServiceKeyConfig;

@Service
public class OpenManageJobService implements AsyncService{

   private static final Logger logger = LoggerFactory.getLogger(OpenManageJobService.class);
   @Autowired
   private WormholeAPIClient wormholeApiClient;
   @Autowired
   private StringRedisTemplate template;
   @Autowired
   private ServiceKeyConfig serviceKeyConfig;
   private ObjectMapper mapper = new ObjectMapper();

   @Override
   public void executeAsync(EventMessage message) {
      // TODO Auto-generated method stub
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
            excuteJob(payloadCommand.getId(), integration);
         }
      }
   }

   public void excuteJob(String commanId,FacilitySoftwareConfig integration) {
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
      checkConnection(client,integration);
      //put your sync metadata logic here
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

   private void updateIntegrationStatus(FacilitySoftwareConfig integration) {
      wormholeApiClient.updateFacility(integration);
   }

   private void checkConnection(OpenManageAPIClient client, FacilitySoftwareConfig integration) {
      try {
         client.checkConnection();
      } catch (ResourceAccessException e1) {
         if (e1.getCause().getCause() instanceof ConnectException) {
            checkAndUpdateIntegrationStatus(integration, e1.getMessage());
            return;
         }
      } catch (HttpClientErrorException e) {
         logger.error("Failed to query data from customer adapter", e);
         IntegrationStatus integrationStatus = integration.getIntegrationStatus();
         if (integrationStatus == null) {
            integrationStatus = new IntegrationStatus();
         }
         integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
         integrationStatus.setDetail(e.getMessage());
         integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
         updateIntegrationStatus(integration);
         return;
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

   public OpenManageAPIClient createClient(FacilitySoftwareConfig integration) {
      return new OpenManageAPIClient(integration);
   }
}
