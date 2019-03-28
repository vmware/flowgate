/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class VcSummary {
   private String name = "";
   private String url = "";
   private int hostsNum = 0;

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

   public int getHostsNum() {
      return hostsNum;
   }

   public void setHostsNum(int hostsNum) {
      this.hostsNum = hostsNum;
   }


}
