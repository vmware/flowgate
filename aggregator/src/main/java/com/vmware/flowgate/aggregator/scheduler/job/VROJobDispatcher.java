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

public class VROJobDispatcher extends BaseJob implements Job {

   private static final Logger logger = LoggerFactory.getLogger(VROJobDispatcher.class);

   @Autowired
   private StringRedisTemplate template;
   @Autowired
   private WormholeAPIClient restClient;
   @Autowired
   private ServiceKeyConfig serviceKeyConfig;
   @Autowired
   private MessagePublisher publisher;
   private ObjectMapper mapper = new ObjectMapper();

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {

      String execountString = template.opsForValue().get(EventMessageUtil.VRO_EXECOUNT);
      if (execountString == null || "".equals(execountString)) {
         execountString = "0";
      }
      long execount = Long.valueOf(execountString);

      boolean syncMetriAlertPropertyDefinition = (execount++ % 288000 == 0);//Will you run 1000 days?
      logger.info("Send Sync VRO metric data commands");
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      try {
         template.opsForValue().set(EventMessageUtil.VRO_EXECOUNT, String.valueOf(execount));
      }catch(Exception e) {
         logger.error("Failed to set execount", e);
      }
      SDDCSoftwareConfig[] vroServers = restClient.getInternalSDDCSoftwareConfigByType(SDDCSoftwareConfig.SoftwareType.VRO).getBody();
      if(vroServers == null || vroServers.length==0) {
         logger.info("No VROps server find");
         return;
      }
      try {
         template.opsForList().leftPushAll(EventMessageUtil.vroJobList,
               EventMessageUtil.generateSDDCMessageListByType(EventType.VROps,
                     EventMessageUtil.VRO_SyncMetricData, vroServers));
         if (syncMetriAlertPropertyDefinition) {
            logger.info("Send Sync VRO metric alert property definition commands");
            template.opsForList().leftPushAll(EventMessageUtil.vroJobList,
                  EventMessageUtil.generateSDDCMessageListByType(EventType.VROps,
                        EventMessageUtil.VRO_SyncMetricPropertyAndAlert, vroServers));
         }
         publisher.publish(EventMessageUtil.VROTopic,
               EventMessageUtil.generateSDDCNotifyMessage(EventType.VROps));
      } catch (IOException e) {
         logger.error("Failed to sendout VRO jobs", e);
      }
   }
}
