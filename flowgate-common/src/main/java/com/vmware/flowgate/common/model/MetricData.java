/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class MetricData {
   private String metricName;
   private double valueNum;
   private String value;
   private String unit;
   private long timeStamp;

   public String getMetricName() {
      return metricName;
   }

   public void setMetricName(String metricName) {
      this.metricName = metricName;
   }

   public double getValueNum() {
      return valueNum;
   }

   public void setValueNum(double valueNum) {
      this.valueNum = valueNum;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public long getTimeStamp() {
      return timeStamp;
   }

   public void setTimeStamp(long timeStamp) {
      this.timeStamp = timeStamp;
   }

   public String getUnit() {
      return unit;
   }

   public void setUnit(String unit) {
      this.unit = unit;
   }

}
