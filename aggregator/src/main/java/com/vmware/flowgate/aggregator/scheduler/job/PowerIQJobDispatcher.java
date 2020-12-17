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

public class PowerIQJobDispatcher extends BaseJob implements Job {

   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;


   @Autowired
   private StringRedisTemplate template;

   @Autowired
   private MessagePublisher publisher;

   private static final Logger logger = LoggerFactory.getLogger(PowerIQJobDispatcher.class);

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {
   // Read all the PowerIQ information, send to the redis topic
      //this job will be triggered every 5 minutes for realtime job
      //every day for full sync job.
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      String execountString = template.opsForValue().get(EventMessageUtil.POWERIQ_EXECOUNT);
      if (execountString == null || "".equals(execountString)) {
         execountString = "0";
      }
      long execount = Long.valueOf(execountString);
      boolean fullSync = execount++ % 288 == 0;
      try {
         template.opsForValue().set(EventMessageUtil.POWERIQ_EXECOUNT, String.valueOf(execount));
      }catch(Exception e) {
         logger.error("Failed to set execount", e);
      }
      FacilitySoftwareConfig[] powerIQs = restClient.getFacilitySoftwareInternalByType(SoftwareType.PowerIQ).getBody();
      if(powerIQs ==null || powerIQs.length==0) {
         logger.info("No PowerIQ server find");
         return;
      }
      try {
         template.opsForList().leftPushAll(EventMessageUtil.powerIQJobList,
               EventMessageUtil.generateFacilityMessageListByType(EventType.PowerIQ,
                     EventMessageUtil.PowerIQ_SyncRealtimeData, powerIQs));
         if (fullSync) {
            logger.info("Send Sync PowerIQ sensor metadata command");
            template.opsForList().leftPushAll(EventMessageUtil.powerIQJobList,
                  EventMessageUtil.generateFacilityMessageListByType(EventType.PowerIQ,
                        EventMessageUtil.PowerIQ_SyncAssetsMetaData, powerIQs));
         }
         publisher.publish(EventMessageUtil.POWERIQTopic,
               EventMessageUtil.generateFacilityNotifyMessage(EventType.PowerIQ));
      }catch(IOException e) {
         logger.error("Failed to send out message", e);
      }
      logger.info("Sync PowerIQ findished");
   }
}
