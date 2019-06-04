/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SensorReading {

   private long id;
   @JsonProperty(value = "reading_time")
   private String readingTime;
   private Double value;
   @JsonProperty(value = "sensor_id")
   private Long sensorId;
   @JsonProperty(value = "max_value")
   private Double maxValue;
   @JsonProperty(value = "min_value")
   private Double minValue;
   private String uom;

   public void setId(long id) {
      this.id = id;
   }

   public long getId() {
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

   public Long getSensorId() {
      return sensorId;
   }

   public void setSensorId(Long sensorId) {
      this.sensorId = sensorId;
   }

   public Double getMaxValue() {
      return maxValue;
   }

   public void setMaxValue(Double maxValue) {
      this.maxValue = maxValue;
   }

   public Double getMinValue() {
      return minValue;
   }

   public void setMinValue(Double minValue) {
      this.minValue = minValue;
   }

   public void setUom(String uom) {
      this.uom = uom;
   }

   public String getUom() {
      return uom;
   }

}
