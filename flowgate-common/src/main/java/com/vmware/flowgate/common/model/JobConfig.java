/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import org.springframework.data.annotation.Id;

public class JobConfig {
   @Id
   private String id;
   private String jobName;
   private String jobGroup;
   private String triggerGroup;
   private String triggerName;
   private String jobInfo;
   private String cronExpression;
   private String jobClass;
   private JobType jobType;

   public JobConfig() {
   }

   public JobConfig(String id, String jobName, String jobGroup, String triggerGroup,
         String triggerName, String jobInfo, String cronExpression, String jobClass, JobType jobType) {
      super();
      this.id = id;
      this.jobName = jobName;
      this.jobGroup = jobGroup;
      this.triggerGroup = triggerGroup;
      this.triggerName = triggerName;
      this.jobInfo = jobInfo;
      this.cronExpression = cronExpression;
      this.jobClass = jobClass;
      this.jobType = jobType;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getJobName() {
      return jobName;
   }

   public void setJobName(String jobName) {
      this.jobName = jobName;
   }

   public String getJobGroup() {
      return jobGroup;
   }

   public void setJobGroup(String jobGroup) {
      this.jobGroup = jobGroup;
   }

   public String getJobInfo() {
      return jobInfo;
   }

   public void setJobInfo(String jobInfo) {
      this.jobInfo = jobInfo;
   }

   public String getCronExpression() {
      return cronExpression;
   }

   public void setCronExpression(String cronExpression) {
      this.cronExpression = cronExpression;
   }

   public String getJobClass() {
      return jobClass;
   }

   public void setJobClass(String jobClass) {
      this.jobClass = jobClass;
   }

   public String getTriggerGroup() {
      return triggerGroup;
   }

   public void setTriggerGroup(String triggerGroup) {
      this.triggerGroup = triggerGroup;
   }

   public String getTriggerName() {
      return triggerName;
   }

   public void setTriggerName(String triggerName) {
      this.triggerName = triggerName;
   }

   public enum JobType{
      VRO,VCENTER,AGGREGATOR,OTHER,NLYTE,POWERIQ
   }

   public JobType getJobType() {
      return jobType;
   }

   public void setJobType(JobType jobType) {
      this.jobType = jobType;
   }
}
