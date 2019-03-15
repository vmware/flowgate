/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.common.model;


public class AssetIPMapping {

   private String id;
   private String ip;
   private String assetname;
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


}