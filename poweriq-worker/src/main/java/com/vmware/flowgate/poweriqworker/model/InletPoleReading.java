/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InletPoleReading {

   private long id;
   @JsonProperty(value = "reading_time")
   private String readingTime;
   private Double current;
   @JsonProperty(value = "unutilized_capacity")
   private Double unutilizedCapacity;
   @JsonProperty(value = "pdu_id")
   private Long pduId;
   @JsonProperty(value = "max_current")
   private String maxCurrent;
   @JsonProperty(value = "min_current")
   private String minCurrent;
   @JsonProperty(value = "inlet_pole_id")
   private long inlet_pole_id;
   private Double voltage;
   @JsonProperty(value = "min_voltage")
   private Double minVoltage;
   @JsonProperty(value = "max_voltage")
   private Double maxVoltage;
   @JsonProperty(value = "min_unutilized_capacity")
   private String minUnutilizedCapacity;
   @JsonProperty(value = "max_unutilized_capacity")
   private String maxUnutilizedCapacity;
   @JsonProperty(value = "inlet_id")
   private long inletId;
   @JsonProperty(value = "inlet_ordinal")
   private long inletOrdinal;
   @JsonProperty(value = "inlet_pole_ordinal")
   private long inletPoleOrdinal;

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

   public Double getCurrent() {
      return current;
   }

   public void setCurrent(Double current) {
      this.current = current;
   }

   public Double getUnutilizedCapacity() {
      return unutilizedCapacity;
   }

   public void setUnutilizedCapacity(Double unutilizedCapacity) {
      this.unutilizedCapacity = unutilizedCapacity;
   }

   public Long getPduId() {
      return pduId;
   }

   public void setPduId(Long pduId) {
      this.pduId = pduId;
   }

   public String getMaxCurrent() {
      return maxCurrent;
   }

   public void setMaxCurrent(String maxCurrent) {
      this.maxCurrent = maxCurrent;
   }

   public String getMinCurrent() {
      return minCurrent;
   }

   public void setMinCurrent(String minCurrent) {
      this.minCurrent = minCurrent;
   }

   public long getInlet_pole_id() {
      return inlet_pole_id;
   }

   public void setInlet_pole_id(long inlet_pole_id) {
      this.inlet_pole_id = inlet_pole_id;
   }

   public Double getVoltage() {
      return voltage;
   }

   public void setVoltage(Double voltage) {
      this.voltage = voltage;
   }

   public Double getMinVoltage() {
      return minVoltage;
   }

   public void setMinVoltage(Double minVoltage) {
      this.minVoltage = minVoltage;
   }

   public Double getMaxVoltage() {
      return maxVoltage;
   }

   public void setMaxVoltage(Double maxVoltage) {
      this.maxVoltage = maxVoltage;
   }

   public String getMinUnutilizedCapacity() {
      return minUnutilizedCapacity;
   }

   public void setMinUnutilizedCapacity(String minUnutilizedCapacity) {
      this.minUnutilizedCapacity = minUnutilizedCapacity;
   }

   public String getMaxUnutilizedCapacity() {
      return maxUnutilizedCapacity;
   }

   public void setMaxUnutilizedCapacity(String maxUnutilizedCapacity) {
      this.maxUnutilizedCapacity = maxUnutilizedCapacity;
   }

   public long getInletId() {
      return inletId;
   }

   public void setInletId(long inletId) {
      this.inletId = inletId;
   }

   public long getInletOrdinal() {
      return inletOrdinal;
   }

   public void setInletOrdinal(long inletOrdinal) {
      this.inletOrdinal = inletOrdinal;
   }

   public long getInletPoleOrdinal() {
      return inletPoleOrdinal;
   }

   public void setInletPoleOrdinal(long inletPoleOrdinal) {
      this.inletPoleOrdinal = inletPoleOrdinal;
   }

}