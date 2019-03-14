/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InletReading {

   private int id;
   @JsonProperty(value = "pdu_id")
   private Integer pduId;
   @JsonProperty(value = "inlet_id")
   private int inletId;
   @JsonProperty(value = "reading_time")
   private String readingTime;
   private Double voltage;
   @JsonProperty(value = "min_voltage")
   private Double minVoltage;
   @JsonProperty(value = "max_voltage")
   private Double maxVoltage;
   private Double current;
   @JsonProperty(value = "max_current")
   private String maxCurrent;
   @JsonProperty(value = "min_current")
   private String minCurrent;
   @JsonProperty(value = "unutilized_capacity")
   private Double unutilizedCapacity;
   @JsonProperty(value = "min_unutilized_capacity")
   private String minUnutilizedCapacity;
   @JsonProperty(value = "max_unutilized_capacity")
   private String maxUnutilizedCapacity;
   @JsonProperty(value = "power_factor")
   private String powerFactor;
   @JsonProperty(value = "min_power_factor")
   private String minPowerFactor;
   @JsonProperty(value = "max_power_factor")
   private String maxPowerFactor;
   @JsonProperty(value = "active_power")
   private Double activePower;
   @JsonProperty(value = "min_active_power")
   private Double minActivePower;
   @JsonProperty(value = "max_active_power")
   private Double maxActivePower;
   @JsonProperty(value = "apparent_power")
   private Double apparentPower;
   @JsonProperty(value = "min_apparent_power")
   private String minApparentPower;
   @JsonProperty(value = "max_apparent_power")
   private String maxApparentPower;
   @JsonProperty(value = "volt_amp_hour")
   private String voltAmpHour;
   @JsonProperty(value = "watt_hour")
   private String wattHour;
   @JsonProperty(value = "watt_hour_delta")
   private String wattHourDelta;
   @JsonProperty(value = "inlet_ordinal")
   private int inletOrdinal;

   public void setId(int id) {
      this.id = id;
   }

   public int getId() {
      return id;
   }

   public Integer getPduId() {
      return pduId;
   }

   public void setPduId(Integer pduId) {
      this.pduId = pduId;
   }

   public int getInletId() {
      return inletId;
   }

   public void setInletId(int inletId) {
      this.inletId = inletId;
   }

   public String getReadingTime() {
      return readingTime;
   }

   public void setReadingTime(String readingTime) {
      this.readingTime = readingTime;
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

   public Double getCurrent() {
      return current;
   }

   public void setCurrent(Double current) {
      this.current = current;
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

   public Double getUnutilizedCapacity() {
      return unutilizedCapacity;
   }

   public void setUnutilizedCapacity(Double unutilizedCapacity) {
      this.unutilizedCapacity = unutilizedCapacity;
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

   public String getPowerFactor() {
      return powerFactor;
   }

   public void setPowerFactor(String powerFactor) {
      this.powerFactor = powerFactor;
   }

   public String getMinPowerFactor() {
      return minPowerFactor;
   }

   public void setMinPowerFactor(String minPowerFactor) {
      this.minPowerFactor = minPowerFactor;
   }

   public String getMaxPowerFactor() {
      return maxPowerFactor;
   }

   public void setMaxPowerFactor(String maxPowerFactor) {
      this.maxPowerFactor = maxPowerFactor;
   }

   public Double getActivePower() {
      return activePower;
   }

   public void setActivePower(Double activePower) {
      this.activePower = activePower;
   }

   public Double getMinActivePower() {
      return minActivePower;
   }

   public void setMinActivePower(Double minActivePower) {
      this.minActivePower = minActivePower;
   }

   public Double getMaxActivePower() {
      return maxActivePower;
   }

   public void setMaxActivePower(Double maxActivePower) {
      this.maxActivePower = maxActivePower;
   }

   public Double getApparentPower() {
      return apparentPower;
   }

   public void setApparentPower(Double apparentPower) {
      this.apparentPower = apparentPower;
   }

   public String getMinApparentPower() {
      return minApparentPower;
   }

   public void setMinApparentPower(String minApparentPower) {
      this.minApparentPower = minApparentPower;
   }

   public String getMaxApparentPower() {
      return maxApparentPower;
   }

   public void setMaxApparentPower(String maxApparentPower) {
      this.maxApparentPower = maxApparentPower;
   }

   public String getVoltAmpHour() {
      return voltAmpHour;
   }

   public void setVoltAmpHour(String voltAmpHour) {
      this.voltAmpHour = voltAmpHour;
   }

   public String getWattHour() {
      return wattHour;
   }

   public void setWattHour(String wattHour) {
      this.wattHour = wattHour;
   }

   public String getWattHourDelta() {
      return wattHourDelta;
   }

   public void setWattHourDelta(String wattHourDelta) {
      this.wattHourDelta = wattHourDelta;
   }

   public int getInletOrdinal() {
      return inletOrdinal;
   }

   public void setInletOrdinal(int inletOrdinal) {
      this.inletOrdinal = inletOrdinal;
   }

}
