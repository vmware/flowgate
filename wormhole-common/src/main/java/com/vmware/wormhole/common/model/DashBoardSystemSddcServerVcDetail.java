/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.common.model;

public class DashBoardSystemSddcServerVcDetail {
   private String sddcName = "";
   private String sddcUrl = "";
   private int num = 0;

   public String getSddcName() {
      return sddcName;
   }

   public void setSddcName(String sddcName) {
      this.sddcName = sddcName;
   }

   public String getSddcUrl() {
      return sddcUrl;
   }

   public void setSddcUrl(String sddcUrl) {
      this.sddcUrl = sddcUrl;
   }

   public int getNum() {
      return num;
   }

   public void setNum(int num) {
      this.num = num;
   }


}
