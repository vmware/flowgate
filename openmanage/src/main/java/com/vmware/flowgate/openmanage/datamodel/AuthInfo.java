/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthInfo {

   @JsonProperty(value="UserName")
   private String UserName;
   @JsonProperty(value="Password")
   private String Password;
   @JsonProperty(value="SessionType")
   private String SessionType;

   public AuthInfo(String userName, String password, String sessionType) {
      this.UserName = userName;
      this.Password = password;
      this.SessionType = sessionType;
   }

   @JsonIgnore
   public String getUserName() {
      return this.UserName;
   }

   public void setUserName(String userName) {
      this.UserName = userName;
   }

   @JsonIgnore
   public String getPassword() {
      return this.Password;
   }

   public void setPassword(String password) {
      this.Password = password;
   }

   @JsonIgnore
   public String getSessionType() {
      return this.SessionType;
   }

   public void setSessionType(String sessionType) {
      this.SessionType = sessionType;
   }

}
