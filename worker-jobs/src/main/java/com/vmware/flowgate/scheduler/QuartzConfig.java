/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.scheduler;

import org.quartz.Scheduler;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
@Configuration
public class QuartzConfig {
   @Bean(name = "scheduler")
   public Scheduler scheduler(QuartzJobFactory quartzJobFactory) throws Exception {

      SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
      factoryBean.setJobFactory(quartzJobFactory);
      factoryBean.afterPropertiesSet();
      Scheduler scheduler = factoryBean.getScheduler();
      scheduler.start();
      return scheduler;
   }
   
   @Component("quartzJobFactory")
   private class QuartzJobFactory extends AdaptableJobFactory {
      @Autowired
      private AutowireCapableBeanFactory capableBeanFactory;

      protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {

         Object jobInstance = super.createJobInstance(bundle);
         capableBeanFactory.autowireBean(jobInstance);
         return jobInstance;
      }
   }
}
