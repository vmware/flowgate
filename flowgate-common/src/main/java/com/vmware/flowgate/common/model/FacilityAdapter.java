/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;

public class FacilityAdapter implements BaseDocument {

   @Id
   private String id;
   private String displayName;
   private String description;
   private String topic;
   private String queueName;
   private SoftwareType type;
   private String subCategory;
   private List<AdapterJobCommand> commands;
   private long createTime;
   private String serviceKey;

   @Override
   public String getId() {
      return id;
   }

   @Override
   public void setId(String id) {
      this.id = id;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getTopic() {
      return topic;
   }

   public void setTopic(String topic) {
      this.topic = topic;
   }

   public String getQueueName() {
      return queueName;
   }

   public void setQueueName(String queueName) {
      this.queueName = queueName;
   }

   public List<AdapterJobCommand> getCommands() {
      return commands;
   }

   public void setCommands(List<AdapterJobCommand> commands) {
      this.commands = commands;
   }

   public String getDisplayName() {
      return displayName;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public long getCreateTime() {
      return createTime;
   }

   public void setCreateTime(long createTime) {
      this.createTime = createTime;
   }

   public SoftwareType getType() {
      return type;
   }

   public void setType(SoftwareType type) {
      this.type = type;
   }

   public String getSubCategory() {
      return subCategory;
   }

   public void setSubCategory(String subCategory) {
      this.subCategory = subCategory;
   }

   public String getServiceKey() {
      return serviceKey;
   }

   public void setServiceKey(String serviceKey) {
      this.serviceKey = serviceKey;
   }

}
