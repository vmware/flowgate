/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceMetric {

   @JsonProperty(value="Type")
   private int type;
   @JsonProperty(value="Value")
   private double value;
   @JsonProperty(value="Timestamp")
   private String timestamp;
   public int getType() {
      return type;
   }
   public void setType(int type) {
      this.type = type;
   }
   public double getValue() {
      return value;
   }
   public void setValue(double value) {
      this.value = value;
   }
   public String getTimestamp() {
      return timestamp;
   }
   public void setTimestamp(String timestamp) {
      this.timestamp = timestamp;
   }
}
