/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PowerManageMetricsRequestBody {

   @JsonProperty(value="PluginId")
   private String pluginId;
   @JsonProperty(value="EntityType")
   private int entityType;
   @JsonProperty(value="EntityId")
   private int entityId;
   @JsonProperty(value="MetricTypes")
   private List<Integer> metricTypes;
   @JsonProperty(value="Duration")
   private int duration;
   @JsonProperty(value="SortOrder")
   private int sortOrder;

   public void setPluginId(String pluginId) {
      this.pluginId = pluginId;
   }
   public void setEntityType(int entityType) {
      this.entityType = entityType;
   }
   public void setEntityId(int entityId) {
      this.entityId = entityId;
   }
   public void setMetricTypes(List<Integer> metricTypes) {
      this.metricTypes = metricTypes;
   }
   public void setDuration(int duration) {
      this.duration = duration;
   }
   public void setSortOrder(int sortOrder) {
      this.sortOrder = sortOrder;
   }
}
