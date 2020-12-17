/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.aggregator.scheduler.job;

import java.io.IOException;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.jobs.BaseJob;

public class NlyteJobDispatcher extends BaseJob implements Job {

   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;

   @Autowired
   private StringRedisTemplate template;

   @Autowired
   private MessagePublisher publisher;

   private static final Logger logger = LoggerFactory.getLogger(NlyteJobDispatcher.class);

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {
      // Read all the Nlyte information, send to the redis topic
      //this job will be triggered every 5 minutes for realtime job
      //every 15 minutes we will trigger a sync mapped data job.
      //every day for full sync job.

      String execountString = template.opsForValue().get(EventMessageUtil.NLYTE_EXECOUNT);
      if (execountString == null || "".equals(execountString)) {
         execountString = "0";
      }
      long execount = Long.valueOf(execountString);
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      boolean fullSync = execount % 288 == 0;
      boolean mappedSync = execount++ % 24 == 0;
      if (fullSync) {
         mappedSync = false;
      }
      logger.info("Send Sync command for Nlyte");
      try {
         template.opsForValue().set(EventMessageUtil.NLYTE_EXECOUNT, String.valueOf(execount));
      }catch(Exception e) {
         logger.error("Failed to set execount", e);
      }
      FacilitySoftwareConfig[] nlytes =
            restClient.getFacilitySoftwareInternalByType(SoftwareType.Nlyte).getBody();
      if (nlytes == null || nlytes.length == 0) {
         logger.info("No Nlyte server find");
         return;
      }
      try {
         template.opsForList().leftPushAll(EventMessageUtil.nlyteJobList,
               EventMessageUtil.generateFacilityMessageListByType(EventType.Nlyte,
                     EventMessageUtil.NLYTE_SyncRealtimeData, nlytes));
         if (fullSync) {
            logger.info("Send Sync Nlyte all assets commands");
            template.opsForList().leftPushAll(EventMessageUtil.nlyteJobList,
                  EventMessageUtil.generateFacilityMessageListByType(EventType.Nlyte,
                        EventMessageUtil.NLYTE_SyncAllAssets, nlytes));
         } else if (mappedSync) {
            logger.info("Send Sync Nlyte mapped assets commands");
            template.opsForList().leftPushAll(EventMessageUtil.nlyteJobList,
                  EventMessageUtil.generateFacilityMessageListByType(EventType.Nlyte,
                        EventMessageUtil.NLYTE_SyncMappedAssetData, nlytes));
         }
         publisher.publish(EventMessageUtil.NLYTETOPIC,
               EventMessageUtil.generateFacilityNotifyMessage(EventType.Nlyte));
      } catch (IOException e) {
         logger.error("Failed to send out message", e);
      }
      logger.info("Finish send sync data command for Nlyte.");
   }
}
