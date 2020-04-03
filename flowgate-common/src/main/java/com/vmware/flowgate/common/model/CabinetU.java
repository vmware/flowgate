/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class CabinetU {

   private boolean used;
   private int cabinetUNumber;
   private String assetsOnUnit;
   public int getCabinetUNumber() {
      return cabinetUNumber;
   }
   public void setCabinetUNumber(int cabinetUNumber) {
      this.cabinetUNumber = cabinetUNumber;
   }
   public boolean isUsed() {
      return used;
   }
   public void setUsed(boolean used) {
      this.used = used;
   }
   public String getAssetsOnUnit() {
      return assetsOnUnit;
   }
   public void setAssetsOnUnit(String assetsOnUnit) {
      this.assetsOnUnit = assetsOnUnit;
   }

}
