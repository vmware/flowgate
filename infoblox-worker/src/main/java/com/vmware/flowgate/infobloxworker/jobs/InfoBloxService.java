/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.infobloxworker.jobs;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import com.vmware.flowgate.infobloxworker.model.InfoBloxIPInfoResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import com.vmware.flowgate.infobloxworker.config.ServiceKeyConfig;
import com.vmware.flowgate.infobloxworker.service.InfobloxClient;
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
      logger.info(String.format("Try to find hostname for ip: %s", message));
      //check message , make sure it is an valid ip address;
      wormholeAPIClient.setServiceKey(serviceKeyConfig.getServiceKey());
      FacilitySoftwareConfig[] infoBloxes =
            wormholeAPIClient.getFacilitySoftwareInternalByType(SoftwareType.InfoBlox).getBody();
      List<InfoBloxIPInfoResult> infoBloxIPInfoResults = new ArrayList<>();
      List<InfoBloxIPInfoResult> hostRecordResults = null;
      List<InfoBloxIPInfoResult> ipv4addressResults = null;
      for (FacilitySoftwareConfig infoblox : infoBloxes) {
         if(!infoblox.checkIsActive()) {
            continue;
         }
         InfobloxClient client = buildInfobloxClient(infoblox);

         IntegrationStatus integrationStatus = infoblox.getIntegrationStatus();
         IntegrationStatus.Status status = integrationStatus == null ? null : integrationStatus.getStatus();
         try {
            if (status == null || IntegrationStatus.Status.ACTIVE.equals(status)) {
               hostRecordResults = client.queryHostRecordByIP(message);
               logger.debug("queryHostRecordByIP: {}", hostRecordResults);
               if (hostRecordResults != null && !hostRecordResults.isEmpty()) {
                  infoBloxIPInfoResults.addAll(hostRecordResults);
               }
            }
            if (hostRecordResults == null || hostRecordResults.isEmpty()) {
               ipv4addressResults = client.queryIpv4addressByIP(message);
               logger.debug("queryHostNamesByIP: {}", ipv4addressResults);
               if (ipv4addressResults != null && !ipv4addressResults.isEmpty()) {
                  infoBloxIPInfoResults.addAll(ipv4addressResults);
               }
               if (!IntegrationStatus.Status.WARNING.equals(status) && ipv4addressResults != null) {
                  updateIntegrationStatusToWarning(infoblox, message);
               }
            }
         }catch(ResourceAccessException e) {
            if(e.getCause().getCause() instanceof ConnectException) {
               checkAndUpdateIntegrationStatus(infoblox, e.getCause().getCause().getMessage());
               continue;
            }
          }catch(HttpClientErrorException e1) {
             logger.error("Failed to query data from Infoblox", e1);
             if(integrationStatus == null) {
                integrationStatus = new IntegrationStatus();
             }
            integrationStatus.setStatus(IntegrationStatus.Status.ERROR);
            integrationStatus.setDetail(e1.getMessage());
            integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
            infoblox.setIntegrationStatus(integrationStatus);
            updateIntegrationStatus(infoblox);
            continue;
          }

         if(integrationStatus != null && integrationStatus.getRetryCounter() > 0) {
        	 integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
        	 updateIntegrationStatus(infoblox);
         }

         if (!infoBloxIPInfoResults.isEmpty()) {
            for (InfoBloxIPInfoResult infoBloxIPInfoResult : infoBloxIPInfoResults) {
               try {
                  Asset asset = wormholeAPIClient.getAssetByName(infoBloxIPInfoResult.getHostName()).getBody();
                  if (asset == null) {
                     logger.info(String.format("hostname (%s) no found!", infoBloxIPInfoResult.getHostName()));
                     continue;
                  }
               } catch (HttpClientErrorException e) {
                  logger.error(String.format("Error when searching %s", infoBloxIPInfoResult.getHostName()), e);
                  continue;
               }
               AssetIPMapping tempMapping = new AssetIPMapping();
               tempMapping.setAssetname(infoBloxIPInfoResult.getHostName());
               tempMapping.setMacAddress(infoBloxIPInfoResult.getMacAddress());
               tempMapping.setIp(message);

               AssetIPMapping[] mappings =
                     wormholeAPIClient.getHostnameIPMappingByIP(message).getBody();
               boolean isNewMapping = true;
               if (null != mappings && mappings.length > 0) {
                  for (AssetIPMapping mapping : mappings) {
                     if (tempMapping.getAssetname().equals(mapping.getAssetname())) {
                        if (!StringUtils.equals(mapping.getMacAddress(), tempMapping.getMacAddress())) {
                           mapping.setMacAddress(tempMapping.getMacAddress());
                           wormholeAPIClient.updateHostnameIPMapping(mapping);
                        }
                        isNewMapping = false;
                        break;
                     }
                  }
               }
               if (isNewMapping) {
                  wormholeAPIClient.createHostnameIPMapping(tempMapping);
               }
               logger.info(String.format("Find hostname %s for ip %s", infoBloxIPInfoResult.getHostName(), message));
               return;
            }
         }
      }
      logger.info(String.format("Cannot find the hostname for IP: %s", message));
   }

   public InfobloxClient buildInfobloxClient(FacilitySoftwareConfig infoblox) {
      return new InfobloxClient(infoblox);
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
      infoblox.setIntegrationStatus(integrationStatus);
      updateIntegrationStatus(infoblox);
   }
   
   private void updateIntegrationStatus(FacilitySoftwareConfig infoblox) {
      wormholeAPIClient.setServiceKey(serviceKeyConfig.getServiceKey());
      wormholeAPIClient.updateFacility(infoblox);
   }

   private void updateIntegrationStatusToWarning (FacilitySoftwareConfig infoblox, String ip) {
      IntegrationStatus integrationStatus = infoblox.getIntegrationStatus();
      if (integrationStatus == null) {
         integrationStatus = new IntegrationStatus();
      }
      integrationStatus.setStatus(IntegrationStatus.Status.WARNING);
      integrationStatus.setDetail(String.format("Unauthorized or ip:%s unmanaged", ip));
      infoblox.setIntegrationStatus(integrationStatus);
      updateIntegrationStatus(infoblox);
      logger.error("Update the infoblox status to warning because there is no permission or the ip unmanaged ip:{}", ip);
   }

}
