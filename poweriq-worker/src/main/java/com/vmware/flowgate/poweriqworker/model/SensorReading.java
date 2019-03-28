/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SensorReading {

   private int id;
   @JsonProperty(value = "reading_time")
   private String readingTime;
   private Double value;
   @JsonProperty(value = "sensor_id")
   private Integer sensorId;
   @JsonProperty(value = "max_value")
   private String maxValue;
   @JsonProperty(value = "min_value")
   private String minValue;
   private String uom;

   public void setId(int id) {
      this.id = id;
   }

   public int getId() {
      return id;
   }

   public String getReadingTime() {
      return readingTime;
   }

   public void setReadingTime(String readingTime) {
      this.readingTime = readingTime;
   }

   public Double getValue() {
      return value;
   }

   public void setValue(Double value) {
      this.value = value;
   }

   public Integer getSensorId() {
      return sensorId;
   }

   public void setSensorId(Integer sensorId) {
      this.sensorId = sensorId;
   }

   public String getMaxValue() {
      return maxValue;
   }

   public void setMaxValue(String maxValue) {
      this.maxValue = maxValue;
   }

   public String getMinValue() {
      return minValue;
   }

   public void setMinValue(String minValue) {
      this.minValue = minValue;
   }

   public void setUom(String uom) {
      this.uom = uom;
   }

   public String getUom() {
      return uom;
   }

}
