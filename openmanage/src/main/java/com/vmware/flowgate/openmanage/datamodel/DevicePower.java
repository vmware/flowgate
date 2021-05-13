/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DevicePower {

   @JsonProperty(value="@odata.context")
   private String odataContext;
   @JsonProperty(value="@odata.type")
   private String odataType;
   @JsonProperty(value="@odata.id")
   private String odataId;
   @JsonProperty(value="avgPower")
   private String avgPower;
   @JsonProperty(value="peakHeadroomUnit")
   private String peakHeadroomUnit;
   @JsonProperty(value="systemEnergyConsumptionUnit")
   private String systemEnergyConsumptionUnit;
   @JsonProperty(value="instantaneousHeadroomTimeStamp")
   private String instantaneousHeadroomTimeStamp;
   @JsonProperty(value="systemEnergyConsumptionTimeStamp")
   private String systemEnergyConsumptionTimeStamp;
   @JsonProperty(value="powerUnit")
   private String powerUnit;
   @JsonProperty(value="minimumPowerTimeStamp")
   private String minimumPowerTimeStamp;
   @JsonProperty(value="avgPowerUnit")
   private String avgPowerUnit;
   @JsonProperty(value="DurationTime")
   private String durationTime;
   @JsonProperty(value="peakHeadroomTimeStamp")
   private String peakHeadroomTimeStamp;
   @JsonProperty(value="instantaneousHeadroomUnit")
   private String instantaneousHeadroomUnit;
   @JsonProperty(value="minimumPower")
   private String minimumPower;
   @JsonProperty(value="peakPower")
   private String peakPower;
   @JsonProperty(value="DateFormat")
   private String dateFormat;
   @JsonProperty(value="systemEnergyConsumption")
   private String systemEnergyConsumption;
   @JsonProperty(value="Since")
   private String since;
   @JsonProperty(value="peakPowerTimeStamp")
   private String peakPowerTimeStamp;
   @JsonProperty(value="power")
   private String power;
   @JsonProperty(value="peakPowerUnit")
   private String peakPowerUnit;
   @JsonProperty(value="instantaneousHeadroom")
   private int instantaneousHeadroom;
   @JsonProperty(value="peakHeadroom")
   private int peakHeadroom;
   @JsonProperty(value="minimumPowerUnit")
   private String minimumPowerUnit;
   public String getOdataContext() {
      return odataContext;
   }
   public void setOdataContext(String odataContext) {
      this.odataContext = odataContext;
   }
   public String getOdataType() {
      return odataType;
   }
   public void setOdataType(String odataType) {
      this.odataType = odataType;
   }
   public String getOdataId() {
      return odataId;
   }
   public void setOdataId(String odataId) {
      this.odataId = odataId;
   }
   public String getAvgPower() {
      return avgPower;
   }
   public void setAvgPower(String avgPower) {
      this.avgPower = avgPower;
   }
   public String getPeakHeadroomUnit() {
      return peakHeadroomUnit;
   }
   public void setPeakHeadroomUnit(String peakHeadroomUnit) {
      this.peakHeadroomUnit = peakHeadroomUnit;
   }
   public String getSystemEnergyConsumptionUnit() {
      return systemEnergyConsumptionUnit;
   }
   public void setSystemEnergyConsumptionUnit(String systemEnergyConsumptionUnit) {
      this.systemEnergyConsumptionUnit = systemEnergyConsumptionUnit;
   }
   public String getInstantaneousHeadroomTimeStamp() {
      return instantaneousHeadroomTimeStamp;
   }
   public void setInstantaneousHeadroomTimeStamp(String instantaneousHeadroomTimeStamp) {
      this.instantaneousHeadroomTimeStamp = instantaneousHeadroomTimeStamp;
   }
   public String getSystemEnergyConsumptionTimeStamp() {
      return systemEnergyConsumptionTimeStamp;
   }
   public void setSystemEnergyConsumptionTimeStamp(String systemEnergyConsumptionTimeStamp) {
      this.systemEnergyConsumptionTimeStamp = systemEnergyConsumptionTimeStamp;
   }
   public String getPowerUnit() {
      return powerUnit;
   }
   public void setPowerUnit(String powerUnit) {
      this.powerUnit = powerUnit;
   }
   public String getMinimumPowerTimeStamp() {
      return minimumPowerTimeStamp;
   }
   public void setMinimumPowerTimeStamp(String minimumPowerTimeStamp) {
      this.minimumPowerTimeStamp = minimumPowerTimeStamp;
   }
   public String getAvgPowerUnit() {
      return avgPowerUnit;
   }
   public void setAvgPowerUnit(String avgPowerUnit) {
      this.avgPowerUnit = avgPowerUnit;
   }
   public String getDurationTime() {
      return durationTime;
   }
   public void setDurationTime(String durationTime) {
      this.durationTime = durationTime;
   }
   public String getPeakHeadroomTimeStamp() {
      return peakHeadroomTimeStamp;
   }
   public void setPeakHeadroomTimeStamp(String peakHeadroomTimeStamp) {
      this.peakHeadroomTimeStamp = peakHeadroomTimeStamp;
   }
   public String getInstantaneousHeadroomUnit() {
      return instantaneousHeadroomUnit;
   }
   public void setInstantaneousHeadroomUnit(String instantaneousHeadroomUnit) {
      this.instantaneousHeadroomUnit = instantaneousHeadroomUnit;
   }
   public String getMinimumPower() {
      return minimumPower;
   }
   public void setMinimumPower(String minimumPower) {
      this.minimumPower = minimumPower;
   }
   public String getPeakPower() {
      return peakPower;
   }
   public void setPeakPower(String peakPower) {
      this.peakPower = peakPower;
   }
   public String getDateFormat() {
      return dateFormat;
   }
   public void setDateFormat(String dateFormat) {
      this.dateFormat = dateFormat;
   }
   public String getSystemEnergyConsumption() {
      return systemEnergyConsumption;
   }
   public void setSystemEnergyConsumption(String systemEnergyConsumption) {
      this.systemEnergyConsumption = systemEnergyConsumption;
   }
   public String getSince() {
      return since;
   }
   public void setSince(String since) {
      this.since = since;
   }
   public String getPeakPowerTimeStamp() {
      return peakPowerTimeStamp;
   }
   public void setPeakPowerTimeStamp(String peakPowerTimeStamp) {
      this.peakPowerTimeStamp = peakPowerTimeStamp;
   }
   public String getPower() {
      return power;
   }
   public void setPower(String power) {
      this.power = power;
   }
   public String getPeakPowerUnit() {
      return peakPowerUnit;
   }
   public void setPeakPowerUnit(String peakPowerUnit) {
      this.peakPowerUnit = peakPowerUnit;
   }
   public int getInstantaneousHeadroom() {
      return instantaneousHeadroom;
   }
   public void setInstantaneousHeadroom(int instantaneousHeadroom) {
      this.instantaneousHeadroom = instantaneousHeadroom;
   }
   public int getPeakHeadroom() {
      return peakHeadroom;
   }
   public void setPeakHeadroom(int peakHeadroom) {
      this.peakHeadroom = peakHeadroom;
   }
   public String getMinimumPowerUnit() {
      return minimumPowerUnit;
   }
   public void setMinimumPowerUnit(String minimumPowerUnit) {
      this.minimumPowerUnit = minimumPowerUnit;
   }
}
