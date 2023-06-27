/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import org.springframework.data.annotation.Id;

public class AssetIPMapping implements BaseDocument {

   @Id
   private String id;
   private String ip;
   private String assetname;
   private String macAddress;
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   public String getIp() {
      return ip;
   }
   public void setIp(String ip) {
      this.ip = ip;
   }
   public String getAssetname() {
      return assetname;
   }
   public void setAssetname(String assetname) {
      this.assetname = assetname;
   }
   public String getMacAddress() {
      return macAddress;
   }
   public void setMacAddress(String macAddress) {
      this.macAddress = macAddress;
   }
}
