/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.infobloxworker.jobs;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class MessageExecutorConfig {

   private static final Logger logger = LoggerFactory.getLogger(MessageExecutorConfig.class);

   @Value("${asyncservice.corepool.size:5}")
   private int taskCorePoolSize;

   @Value("${asyncservice.maxpool.size:10}")
   private int maxPoolSize;

   @Value("${asyncservice.queue.capacity:1000}")
   private int queueCapacity;

   @Bean
   public Executor asyncServiceExecutor() {
      logger.info("Start asyncServiceExecutor.");
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(taskCorePoolSize);
      executor.setMaxPoolSize(maxPoolSize);
      executor.setQueueCapacity(queueCapacity);
      executor.setThreadNamePrefix("infoblox-async-Service-");
      executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
      executor.initialize();
      return executor;
   }
}
