/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class PowerIqSummary {
   private String name = "";
   private String url = "";
   private int sensorNum = 0;
   private int pduNum = 0;

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

   public int getSensorNum() {
      return sensorNum;
   }

   public void setSensorNum(int sensorNum) {
      this.sensorNum = sensorNum;
   }

   public int getPduNum() {
      return pduNum;
   }

   public void setPduNum(int pduNum) {
      this.pduNum = pduNum;
   }


}
