/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.common.model;

public class DashBoardSystemPowerIqDetail {
   private String poweriqName = "";
   private String poweriqUrl = "";
   private int poweriqSensor = 0;

   public String getPoweriqUrl() {
      return poweriqUrl;
   }

   public void setPoweriqUrl(String poweriqUrl) {
      this.poweriqUrl = poweriqUrl;
   }

   public String getPoweriqName() {
      return poweriqName;
   }

   public void setPoweriqName(String poweriqName) {
      this.poweriqName = poweriqName;
   }

   public int getPoweriqSensor() {
      return poweriqSensor;
   }

   public void setPoweriqSensor(int poweriqSensor) {
      this.poweriqSensor = poweriqSensor;
   }
}
