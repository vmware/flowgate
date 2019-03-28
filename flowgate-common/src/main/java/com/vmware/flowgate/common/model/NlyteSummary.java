/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class NlyteSummary {
   private String name = "";
   private String url = "";
   private int serverNum = 0;
   private int pduNum = 0;
   private int cabinetNum = 0;
   private int switchNum = 0;
   private int sensorNum = 0;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public int getServerNum() {
      return serverNum;
   }

   public void setServerNum(int serverNum) {
      this.serverNum = serverNum;
   }

   public int getPduNum() {
      return pduNum;
   }

   public void setPduNum(int pduNum) {
      this.pduNum = pduNum;
   }

   public int getCabinetNum() {
      return cabinetNum;
   }

   public void setCabinetNum(int cabinetNum) {
      this.cabinetNum = cabinetNum;
   }

   public int getSwitchNum() {
      return switchNum;
   }

   public void setSwitchNum(int switchNum) {
      this.switchNum = switchNum;
   }

   public int getSensorNum() {
      return sensorNum;
   }

   public void setSensorNum(int sensorNum) {
      this.sensorNum = sensorNum;
   }

}
