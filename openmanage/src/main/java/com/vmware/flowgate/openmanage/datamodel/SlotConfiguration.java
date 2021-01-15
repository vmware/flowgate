/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SlotConfiguration {

   @JsonProperty(value="ChassisName")
   private String chassisName;
   @JsonProperty(value="SlotId")
   private String slotId;
   @JsonProperty(value="DeviceType")
   private String deviceType;
   @JsonProperty(value="ChassisId")
   private String chassisId;
   @JsonProperty(value="SlotNumber")
   private String slotNumber;
   @JsonProperty(value="SledBlockPowerOn")
   private String sledBlockPowerOn;
   @JsonProperty(value="SlotName")
   private String slotName;
   @JsonProperty(value="ChassisServiceTag")
   private String chassisServiceTag;
   @JsonProperty(value="SlotType")
   private String slotType;

   public String getChassisName() {
      return chassisName;
   }
   public void setChassisName(String chassisName) {
      this.chassisName = chassisName;
   }
   public String getSlotId() {
      return slotId;
   }
   public void setSlotId(String slotId) {
      this.slotId = slotId;
   }
   public String getDeviceType() {
      return deviceType;
   }
   public void setDeviceType(String deviceType) {
      this.deviceType = deviceType;
   }
   public String getChassisId() {
      return chassisId;
   }
   public void setChassisId(String chassisId) {
      this.chassisId = chassisId;
   }
   public String getSlotNumber() {
      return slotNumber;
   }
   public void setSlotNumber(String slotNumber) {
      this.slotNumber = slotNumber;
   }
   public String getSledBlockPowerOn() {
      return sledBlockPowerOn;
   }
   public void setSledBlockPowerOn(String sledBlockPowerOn) {
      this.sledBlockPowerOn = sledBlockPowerOn;
   }
   public String getSlotName() {
      return slotName;
   }
   public void setSlotName(String slotName) {
      this.slotName = slotName;
   }
   public String getChassisServiceTag() {
      return chassisServiceTag;
   }
   public void setChassisServiceTag(String chassisServiceTag) {
      this.chassisServiceTag = chassisServiceTag;
   }
   public String getSlotType() {
      return slotType;
   }
   public void setSlotType(String slotType) {
      this.slotType = slotType;
   }

}
