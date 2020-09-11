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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.jobs.BaseJob;

public class VCenterJobDispatcher extends BaseJob implements Job {

   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;

   @Autowired
   private StringRedisTemplate template;

   @Autowired
   private MessagePublisher publisher;
   private ObjectMapper mapper = new ObjectMapper();

   private static final Logger logger = LoggerFactory.getLogger(VCenterJobDispatcher.class);

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {
      // Read all the vcenter information, send to the redis topic
      //this job will be triggered every 5 minutes.
      
      //every 5 minutes we will trigger a sync host realtimedata job.
      //every 12 hour we will trigger a sync host metadata job.
      //every 1 day we will trigger a sync CustomerAttrsData job.
      //every 10 days we will trigger a sync CustomAttributes job.
      
      String execountString = template.opsForValue().get(EventMessageUtil.VCENTER_EXECOUNT);
      if (execountString == null || "".equals(execountString)) {
         execountString = "0";
      }
      long execount = Long.valueOf(execountString);
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      boolean syncHostMetadata = execount % 144 == 0;
      boolean syncCustomerAttrsData = execount % 288 == 0;
      boolean syncCustomAttributes = execount % 2880 == 0;
      execount++;

      try {
         template.opsForValue().set(EventMessageUtil.VCENTER_EXECOUNT, String.valueOf(execount));
      }catch(Exception e) {
         logger.error("Failed to set execount", e);
      }
      SDDCSoftwareConfig[] vcServers = restClient.getVCServers().getBody();
      if (vcServers == null || vcServers.length == 0) {
         logger.info("No vcenter server find");
         return;
      }
      try {
         logger.info("Send query Host meta data commands");
         template.opsForList().leftPushAll(EventMessageUtil.vcJobList,
               EventMessageUtil.generateSDDCMessageListByType(EventType.VCenter,
                     EventMessageUtil.VCENTER_QueryHostMetaData, vcServers));
         if (syncHostMetadata) {
            logger.info("Send query Host usage data commands");
            template.opsForList().leftPushAll(EventMessageUtil.vcJobList,
                  EventMessageUtil.generateSDDCMessageListByType(EventType.VCenter,
                        EventMessageUtil.VCENTER_QueryHostUsageData, vcServers));
         }
         if (syncCustomerAttrsData) {
            logger.info("Send Sync VC customer attributes data commands");
            template.opsForList().leftPushAll(EventMessageUtil.vcJobList,
                  EventMessageUtil.generateSDDCMessageListByType(EventType.VCenter,
                        EventMessageUtil.VCENTER_SyncCustomerAttrsData, vcServers));
         }
         if (syncCustomAttributes) {
            logger.info("Send Sync VC customer attributes commands");
            template.opsForList().leftPushAll(EventMessageUtil.vcJobList,
                  EventMessageUtil.generateSDDCMessageListByType(EventType.VCenter,
                        EventMessageUtil.VCENTER_SyncCustomerAttrs, vcServers));
         }

         publisher.publish(EventMessageUtil.VCTopic,
               EventMessageUtil.generateSDDCNotifyMessage(EventType.VCenter));
      } catch (IOException e) {
         logger.error("Failed to send out message", e);
      }
      logger.info("Sync VC findished");
   }


}
