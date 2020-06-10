/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChassisMountedAssetMap {

   @JsonProperty(value = "MountingSide")
   private String mountingSide;
   @JsonProperty(value = "ChassisMountedAssetMapID")
   private Integer chassisMountedAssetMapID;
   @JsonProperty(value = "ChassisAssetID")
   private Integer chassisAssetID;
   @JsonProperty(value = "MountedAssetID")
   private Integer mountedAssetID;
   @JsonProperty(value = "ColumnPosition")
   private Integer columnPosition;
   @JsonProperty(value = "RowPosition")
   private Integer rowPosition;
   @JsonProperty(value = "SlotName")
   private String slotName;
   public String getMountingSide() {
      return mountingSide;
   }
   public void setMountingSide(String mountingSide) {
      this.mountingSide = mountingSide;
   }
   public Integer getChassisMountedAssetMapID() {
      return chassisMountedAssetMapID;
   }
   public void setChassisMountedAssetMapID(Integer chassisMountedAssetMapID) {
      this.chassisMountedAssetMapID = chassisMountedAssetMapID;
   }
   public Integer getChassisAssetID() {
      return chassisAssetID;
   }
   public void setChassisAssetID(Integer chassisAssetID) {
      this.chassisAssetID = chassisAssetID;
   }
   public Integer getMountedAssetID() {
      return mountedAssetID;
   }
   public void setMountedAssetID(Integer mountedAssetID) {
      this.mountedAssetID = mountedAssetID;
   }
   public Integer getColumnPosition() {
      return columnPosition;
   }
   public void setColumnPosition(Integer columnPosition) {
      this.columnPosition = columnPosition;
   }
   public Integer getRowPosition() {
      return rowPosition;
   }
   public void setRowPosition(Integer rowPosition) {
      this.rowPosition = rowPosition;
   }
   public String getSlotName() {
      return slotName;
   }
   public void setSlotName(String slotName) {
      this.slotName = slotName;
   }

}
