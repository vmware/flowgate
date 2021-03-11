/**
 * Copyright 2021 VMware, Inc.
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

public class OpenmanageJobDispatcher extends BaseJob implements Job {

   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;

   @Autowired
   private StringRedisTemplate template;

   @Autowired
   private MessagePublisher publisher;

   private static final Logger logger = LoggerFactory.getLogger(OpenmanageJobDispatcher.class);

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {
      //Read all the openmanage integrations, send to the redis topic
      //Get metrics data job will be triggered every 5 minutes
      //Get metadata job will be triggered every day.
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      String execountString = template.opsForValue().get(EventMessageUtil.OpenManage_EXECOUNT);
      if (execountString == null || "".equals(execountString)) {
         execountString = "0";
      }
      long execount = Long.valueOf(execountString);
      boolean fullSync = execount++ % 288 == 0;
      try {
         template.opsForValue().set(EventMessageUtil.OpenManage_EXECOUNT, String.valueOf(execount));
      }catch(Exception e) {
         logger.error("Failed to set execount", e);
      }
      FacilitySoftwareConfig[] openmanages = restClient.getFacilitySoftwareInternalByType(SoftwareType.OpenManage).getBody();
      if(openmanages ==null || openmanages.length==0) {
         logger.info("No Openmanage integration find");
         return;
      }
      try {
         template.opsForList().leftPushAll(EventMessageUtil.OpenManageJobList,
               EventMessageUtil.generateFacilityMessageListByType(EventType.OpenManage,
                     EventMessageUtil.OpenManage_SyncRealtimeData, openmanages));
         if (fullSync) {
            logger.info("Send Sync openmanage metadata command");
            template.opsForList().leftPushAll(EventMessageUtil.OpenManageJobList,
                  EventMessageUtil.generateFacilityMessageListByType(EventType.OpenManage,
                        EventMessageUtil.OpenManage_SyncAssetsMetaData, openmanages));
         }
         publisher.publish(EventMessageUtil.OpenManageTopic,
               EventMessageUtil.generateFacilityNotifyMessage(EventType.OpenManage));
      }catch(IOException e) {
         logger.error("Failed to send out message", e);
      }
      logger.info("Findish send sync openmanage data command");
   }
}
