/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vcworker.model;

import com.vmware.flowgate.common.model.SDDCSoftwareConfig;

public class VCServerInfo {
   private String ip;
   private String userName;
   private String password;
   private boolean skipVerifyCert;

   public VCServerInfo(SDDCSoftwareConfig config) {
      this.ip = config.getServerURL();
      this.userName = config.getUserName();
      this.password = config.getPassword();
      this.skipVerifyCert = !config.isVerifyCert();
   }
   public VCServerInfo(String ip, String userName, String password, boolean skipVerifyCert) {
      super();
      this.ip = ip;
      this.userName = userName;
      this.password = password;
      this.skipVerifyCert = skipVerifyCert;
   }

   public String getIp() {
      return ip;
   }

   public void setIp(String ip) {
      this.ip = ip;
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public boolean isSkipVerifyCert() {
      return skipVerifyCert;
   }

   public void setSkipVerifyCert(boolean skipVerifyCert) {
      this.skipVerifyCert = skipVerifyCert;
   }

}
