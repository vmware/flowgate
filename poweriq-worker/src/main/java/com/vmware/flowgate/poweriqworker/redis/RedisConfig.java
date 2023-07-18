/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import com.vmware.flowgate.common.model.redis.message.MessageReceiver;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;

@Configuration
public class RedisConfig {

   @Bean
   RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
         MessageListenerAdapter listenerAdapter) {
      RedisMessageListenerContainer container = new RedisMessageListenerContainer();
      container.setConnectionFactory(connectionFactory);
      container.addMessageListener(listenerAdapter,
            new ChannelTopic(EventMessageUtil.POWERIQTopic));
      return container;
   }

   @Bean
   MessageListenerAdapter listenerAdapter(MessageReceiver receiver) {
      return new MessageListenerAdapter(receiver, "receiveMessage");
   }

   @Bean
   StringRedisTemplate template(RedisConnectionFactory connectionFactory) {
      return new StringRedisTemplate(connectionFactory);
   }
}
