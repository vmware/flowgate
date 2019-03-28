/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.infobloxworker.redis;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;

@Component
public class RedisMessagePublisher implements MessagePublisher {

   @Autowired
   private StringRedisTemplate template;

   private ObjectMapper mapper = new ObjectMapper();
   private static final Logger logger = LoggerFactory.getLogger(RedisMessagePublisher.class);

   @Override
   public void publish(String topic, String message) {
      String publishedMessage = converToEventMessage(message);
      if (null != publishedMessage) {
         template.convertAndSend(topic, message);
      }
   }

   private String converToEventMessage(String message) {
      try {
         return EventMessageUtil.convertToEventMessageAsString(EventType.InfoBlox, message);
      } catch (IOException e) {
         logger.error("Error happens while create event message", e);
         return null;
      }
   }

}
