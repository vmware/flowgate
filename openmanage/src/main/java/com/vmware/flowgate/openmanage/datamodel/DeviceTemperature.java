/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceTemperature {

   @JsonProperty(value="@odata.context")
   private String odataContext;
   @JsonProperty(value="@odata.type")
   private String odataType;
   @JsonProperty(value="@odata.id")
   private String odataId;
   @JsonProperty(value="peakTemperatureUnit")
   private String peakTemperatureUnit;
   @JsonProperty(value="avgTemperatureUnit")
   private String avgTemperatureUnit;
   @JsonProperty(value="DateFormat")
   private String dateFormat;
   @JsonProperty(value="instantaneousTemperatureUnit")
   private String instantaneousTemperatureUnit;
   @JsonProperty(value="startTime")
   private String startTime;
   @JsonProperty(value="avgTemperatureTimeStamp")
   private String avgTemperatureTimeStamp;
   @JsonProperty(value="avgTemperature")
   private String avgTemperature;
   @JsonProperty(value="instantaneousTemperature")
   private String instantaneousTemperature;
   @JsonProperty(value="peakTemperature")
   private String peakTemperature;
   @JsonProperty(value="peakTemperatureTimeStamp")
   private String peakTemperatureTimeStamp;
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
   public String getPeakTemperatureUnit() {
      return peakTemperatureUnit;
   }
   public void setPeakTemperatureUnit(String peakTemperatureUnit) {
      this.peakTemperatureUnit = peakTemperatureUnit;
   }
   public String getAvgTemperatureUnit() {
      return avgTemperatureUnit;
   }
   public void setAvgTemperatureUnit(String avgTemperatureUnit) {
      this.avgTemperatureUnit = avgTemperatureUnit;
   }
   public String getDateFormat() {
      return dateFormat;
   }
   public void setDateFormat(String dateFormat) {
      this.dateFormat = dateFormat;
   }
   public String getInstantaneousTemperatureUnit() {
      return instantaneousTemperatureUnit;
   }
   public void setInstantaneousTemperatureUnit(String instantaneousTemperatureUnit) {
      this.instantaneousTemperatureUnit = instantaneousTemperatureUnit;
   }
   public String getStartTime() {
      return startTime;
   }
   public void setStartTime(String startTime) {
      this.startTime = startTime;
   }
   public String getAvgTemperatureTimeStamp() {
      return avgTemperatureTimeStamp;
   }
   public void setAvgTemperatureTimeStamp(String avgTemperatureTimeStamp) {
      this.avgTemperatureTimeStamp = avgTemperatureTimeStamp;
   }
   public String getAvgTemperature() {
      return avgTemperature;
   }
   public void setAvgTemperature(String avgTemperature) {
      this.avgTemperature = avgTemperature;
   }
   public String getInstantaneousTemperature() {
      return instantaneousTemperature;
   }
   public void setInstantaneousTemperature(String instantaneousTemperature) {
      this.instantaneousTemperature = instantaneousTemperature;
   }
   public String getPeakTemperature() {
      return peakTemperature;
   }
   public void setPeakTemperature(String peakTemperature) {
      this.peakTemperature = peakTemperature;
   }
   public String getPeakTemperatureTimeStamp() {
      return peakTemperatureTimeStamp;
   }
   public void setPeakTemperatureTimeStamp(String peakTemperatureTimeStamp) {
      this.peakTemperatureTimeStamp = peakTemperatureTimeStamp;
   }

}
