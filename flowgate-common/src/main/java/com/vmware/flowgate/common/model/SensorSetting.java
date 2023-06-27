/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import org.springframework.data.annotation.Id;

public class SensorSetting implements BaseDocument {

   @Id
   private String id;
   private String type;
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

   public String getType() {
      return type;
   }

   public void setType(String type) {
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
