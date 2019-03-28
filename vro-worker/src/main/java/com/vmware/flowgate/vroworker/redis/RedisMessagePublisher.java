/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker.redis;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;

@Component
public class RedisMessagePublisher implements MessagePublisher {

   @Autowired
   private StringRedisTemplate template;

   private static final Logger logger = LoggerFactory.getLogger(RedisMessagePublisher.class);
   @Value("${redis.topic:infoblox}")
   private String topic;

   @Override
   public void publish(String topic, String message) {
      if (StringUtils.isEmpty(topic)) {
         topic = this.topic;
      }
      String publishedMessage = converToEventMessage(message);
      if (null != publishedMessage) {
         template.convertAndSend(topic, publishedMessage);
      }
   }

   private String converToEventMessage(String message) {
      try {
         return EventMessageUtil.convertToEventMessageAsString(EventType.VROps, message);
      } catch (IOException e) {
         logger.error("Error happens while create event message", e);
         return null;
      }
   }

}
