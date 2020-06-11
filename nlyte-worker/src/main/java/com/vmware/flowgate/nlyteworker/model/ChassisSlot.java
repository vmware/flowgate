/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChassisSlot {

   @JsonProperty(value = "MountingSide")
   private String MountingSide;
   @JsonProperty(value = "ID")
   private Integer id;
   @JsonProperty(value = "ChassisAssetID")
   private Integer chassisAssetID;
   @JsonProperty(value = "ColumnPosition")
   private Integer columnPosition;
   @JsonProperty(value = "RowPosition")
   private Integer rowPosition;
   @JsonProperty(value = "SlotName")
   private String slotName;
   public String getMountingSide() {
      return MountingSide;
   }
   public void setMountingSide(String mountingSide) {
      MountingSide = mountingSide;
   }
   public Integer getId() {
      return id;
   }
   public void setId(Integer id) {
      this.id = id;
   }
   public Integer getChassisAssetID() {
      return chassisAssetID;
   }
   public void setChassisAssetID(Integer chassisAssetID) {
      this.chassisAssetID = chassisAssetID;
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
