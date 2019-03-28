/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import com.vmware.flowgate.common.model.ServerSensorData.ServerSensorType;

public class SensorSetting {

   private String id;
   private ServerSensorType type;
   private double minNum;
   private double maxNum;
   private String minValue;
   private String maxValue;

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public ServerSensorType getType() {
      return type;
   }

   public void setType(ServerSensorType type) {
      this.type = type;
   }

   public double getMinNum() {
      return minNum;
   }

   public void setMinNum(double minNum) {
      this.minNum = minNum;
   }

   public double getMaxNum() {
      return maxNum;
   }

   public void setMaxNum(double maxNum) {
      this.maxNum = maxNum;
   }

   public String getMinValue() {
      return minValue;
   }

   public void setMinValue(String minValue) {
      this.minValue = minValue;
   }

   public String getMaxValue() {
      return maxValue;
   }

   public void setMaxValue(String maxValue) {
      this.maxValue = maxValue;
   }

}
