/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vcworker.redis;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisJobExecutor implements DisposableBean, Runnable{


   private Thread thread;
   private volatile boolean someCondition;

   @Autowired
   private StringRedisTemplate template;

   RedisJobExecutor(){
       this.thread = new Thread(this);
       this.thread.start();
   }

   @Override
   public void run(){
      while(true){
           System.out.println("Waiting for a message in the queue");
           String job = template.opsForList().leftPop("vc-jobs", 60, TimeUnit.SECONDS);
           System.out.println("received message with key:" + job);
       }
   }

   @Override
   public void destroy(){
       someCondition = false;
   }
}
