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

import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.jobs.BaseJob;

public class AggregatorJobDispatcher extends BaseJob implements Job {

   @Autowired
   private MessagePublisher publisher;
   private static final Logger logger = LoggerFactory.getLogger(AggregatorJobDispatcher.class);
   @Autowired
   private StringRedisTemplate template;

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {
      String execountString = template.opsForValue().get(EventMessageUtil.AGGREGATOR_EXECOUNT);
      if (execountString == null || "".equals(execountString)) {
         execountString = "0";
      }
      long execount = Long.valueOf(execountString);
      //will execute weekly?
      if (execount++ % 168 == 0) {
         try {
            EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.Aggregator,
                  EventMessageUtil.FullMappingCommand, "");
            String message = EventMessageUtil.convertEventMessageAsString(eventMessage);
            publisher.publish(EventMessageUtil.AggregatorTopic, message);
            logger.info("Send full mapping sync command");
         } catch (IOException e) {
            logger.error("Failed to send full mapping sync command", e);
         }
      }else if(execount % 87 == 0) {
         try {
            EventMessage eventMessageForCleanJob = EventMessageUtil.createEventMessage(EventType.Aggregator,
                  EventMessageUtil.CleanRealtimeData, "");
            String cleanJobMessage = EventMessageUtil.convertEventMessageAsString(eventMessageForCleanJob);
            publisher.publish(EventMessageUtil.AggregatorTopic, cleanJobMessage);
            logger.info("Send clean realtime data command");
         }catch(IOException e) {
            logger.error("Failed to Send clean realtime data command", e);
         }

         try {
            EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.Aggregator,
                  EventMessageUtil.AggregateAndCleanPowerIQPDU, "");
            String jobmessage = EventMessageUtil.convertEventMessageAsString(eventMessage);
            publisher.publish(EventMessageUtil.AggregatorTopic, jobmessage);
            logger.info("Send aggregate Pdu data command");
         }catch(IOException e) {
            logger.error("Failed to Send aggregate pdu data command", e);
         }
      }else if(execount % 24 == 0){
         try {
            EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.Aggregator,
                  EventMessageUtil.SUMMARY_DATA, "");
            String jobmessage = EventMessageUtil.convertEventMessageAsString(eventMessage);
            publisher.publish(EventMessageUtil.AggregatorTopic, jobmessage);
            logger.info("Send sync summary data command");
         }catch(IOException e) {
            logger.error("Failed to Send sync summary data command", e);
         }
      }else {
         //will execute hourly?
         try {
            EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.Aggregator,
                  EventMessageUtil.PDUServerMappingCommand, "");
            String message = EventMessageUtil.convertEventMessageAsString(eventMessage);
            publisher.publish(EventMessageUtil.AggregatorTopic, message);
            logger.info("Send pdu servermapping command");
         } catch (IOException e) {
            logger.error("Failed to send pdu server mapping command.", e);
         }
         try {
            EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.Aggregator,
                  EventMessageUtil.SyncTemperatureAndHumiditySensors, "");
            String message = EventMessageUtil.convertEventMessageAsString(eventMessage);
            publisher.publish(EventMessageUtil.AggregatorTopic, message);
            logger.info("Send sensor host mapping sync command.");
         } catch (IOException e) {
            logger.error("Failed to send sensor sync command", e);
         }
      }
      template.opsForValue().set(EventMessageUtil.AGGREGATOR_EXECOUNT, String.valueOf(execount));
   }

}
