/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.aggregator.scheduler.job;

import java.io.IOException;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.wormhole.common.model.redis.message.EventMessage;
import com.vmware.wormhole.common.model.redis.message.EventType;
import com.vmware.wormhole.common.model.redis.message.MessagePublisher;
import com.vmware.wormhole.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.wormhole.jobs.BaseJob;

public class AggregatorJobDispatcher extends BaseJob implements Job {

   @Autowired
   private MessagePublisher publisher;
   private static final Logger logger = LoggerFactory.getLogger(AggregatorJobDispatcher.class);
   private static long execount = 0;

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {
      execount++;
      //will execute hourly?
      try {
         EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.Aggregator,
               EventMessageUtil.PDUServerMappingCommand, "");
         String message = EventMessageUtil.convertEventMessageAsString(eventMessage);
         publisher.publish(EventMessageUtil.AggregatorTopic, message);
         logger.info("Send pdu servermapping command");
      } catch (IOException e) {
         logger.error("Failed to send pdu servermapping command.", e);
      }

      //will execute weekly?
      if (execount % 168 == 0) {
         try {
            EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.Aggregator,
                  EventMessageUtil.FullMappingCommand, "");
            String message = EventMessageUtil.convertEventMessageAsString(eventMessage);
            publisher.publish(EventMessageUtil.AggregatorTopic, message);
            logger.info("Send full mapping sync command");
         } catch (IOException e) {
            logger.error("Failed to send full mapping sync command", e);
         }
      }
   }

}
