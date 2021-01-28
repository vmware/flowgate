/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceMetricsResult {

   @JsonProperty(value="EntityId")
   private int id;
   @JsonProperty(value="Value")
   private List<DeviceMetric> value;
   public int getId() {
      return id;
   }
   public void setId(int id) {
      this.id = id;
   }
   public List<DeviceMetric> getValue() {
      return value;
   }
   public void setValue(List<DeviceMetric> value) {
      this.value = value;
   }
}
