/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class FlowgateChassisSlot {

   private String slotName;
   private String mountingSide;
   private Integer columnPosition;
   private Integer rowPosition;
   private Integer mountedAssetNumber;
   public String getSlotName() {
      return slotName;
   }
   public void setSlotName(String slotName) {
      this.slotName = slotName;
   }
   public String getMountingSide() {
      return mountingSide;
   }
   public void setMountingSide(String mountingSide) {
      this.mountingSide = mountingSide;
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
   public Integer getMountedAssetNumber() {
      return mountedAssetNumber;
   }
   public void setMountedAssetNumber(Integer mountedAssetNumber) {
      this.mountedAssetNumber = mountedAssetNumber;
   }

}
