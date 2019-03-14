/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.infobloxworker.jobs;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.vmware.wormhole.client.WormholeAPIClient;
import com.vmware.wormhole.common.model.Asset;
import com.vmware.wormhole.common.model.AssetIPMapping;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.wormhole.common.model.redis.message.AsyncService;
import com.vmware.wormhole.common.model.redis.message.EventMessage;
import com.vmware.wormhole.common.model.redis.message.MessagePublisher;
import com.vmware.wormhole.infobloxworker.config.ServiceKeyConfig;
import com.vmware.wormhole.infobloxworker.service.InfobloxClient;

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
            wormholeAPIClient.getFacilitySoftwareByType(SoftwareType.InfoBlox).getBody();
      for (FacilitySoftwareConfig infoblox : infoBloxes) {
         InfobloxClient client = new InfobloxClient(infoblox);
         List<String> hostNames = null;
         hostNames = client.queryHostNamesByIP(message);
         if (hostNames != null && !hostNames.isEmpty()) {
            for (String hostname : hostNames) {
               try {
                  ResponseEntity<Asset> asset = wormholeAPIClient.getAssetByName(hostname);
                  if (asset == null) {
                     logger.info(String.format("hostname (%s) no found!", hostname));
                     continue;
                  }
               } catch (HttpClientErrorException e) {
                  logger.error(String.format("Error when searching %s", hostname), e);
                  continue;
               }
               AssetIPMapping tempMapping = new AssetIPMapping();
               tempMapping.setAssetname(hostname);
               tempMapping.setIp(message);

               AssetIPMapping[] mappings =
                     wormholeAPIClient.getHostnameIPMappingByIP(message).getBody();
               boolean isNewMapping = true;
               if (null != mappings && mappings.length > 0) {
                  for (AssetIPMapping mapping : mappings) {
                     if (hostname.equals(mapping.getAssetname())) {
                        isNewMapping = false;
                        break;
                     }
                  }
               }
               if (isNewMapping) {
                  wormholeAPIClient.createHostnameIPMapping(tempMapping);
               }
               logger.info(String.format("Find hostname %s for ip %s", hostname, message));
               return;
            }
         }
      }
      logger.info(String.format("Cannot find the hostname for IP: %s", message));
   }

}
