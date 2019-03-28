/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.aggregator.scheduler.job;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.model.JobConfig;
import com.vmware.flowgate.common.model.JobConfig.JobType;

@Component
@Profile("!test")
public class AggregatorJobLoader implements CommandLineRunner, ApplicationContextAware {

   private final Logger logger = LoggerFactory.getLogger(AggregatorJobLoader.class);
   private ApplicationContext applicationContext;

   @Autowired
   private Scheduler scheduler;

   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;

   @Override
   public void setApplicationContext(ApplicationContext arg0) throws BeansException {
      this.applicationContext = arg0;
   }

   @Override
   public void run(String... args) throws Exception {
      loadAllJobs();
   }

   private void loadAllJobs() throws ClassNotFoundException, SchedulerException {
      logger.debug("Init all predfined jobs");
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      JobConfig[] jobs = null;
      int maxTrytimes=3;//will try 3 time to connect to the wormhole api
      while(maxTrytimes>0) {
         try {
            jobs=restClient.getJobs(JobType.AGGREGATOR).getBody();
            break;
         }catch(Exception e) {
            logger.warn("Failed to connect to wormhole API", e);
            maxTrytimes--;
            try {
               Thread.sleep(20000);//sleep 20 seconds
            }catch(InterruptedException ie) {
               logger.error("Fatal error",ie);
            }
         }
      }

      for (JobConfig job : jobs) {
         Class jobClass = Class.forName(job.getJobClass());
         JobKey jobKey = JobKey.jobKey(job.getJobName(), job.getJobGroup());
         TriggerKey triggerKey = TriggerKey.triggerKey(job.getTriggerName(), job.getTriggerGroup());
         JobDetail jobDetail = newJob(jobClass).withIdentity(jobKey).build();
         Trigger trigger =
               newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                     .withIdentity(triggerKey).build();
         scheduler.scheduleJob(jobDetail, trigger);
      }
   }

   public Scheduler getScheduler() {
      return scheduler;
   }

   public void setScheduler(Scheduler scheduler) {
      this.scheduler = scheduler;
   }

}
