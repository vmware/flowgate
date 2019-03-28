/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionExpriesData {

   @JsonProperty(value="UserAccountId")
   private int userAccountId;
   @JsonProperty(value="UserName")
   private String userName;
   @JsonProperty(value="Expires")
   private long expires;
   @JsonProperty(value="AuthModel")
   private String authModel;

   public int getUserAccountId() {
      return userAccountId;
   }
   public void setUserAccountId(int userAccountId) {
      this.userAccountId = userAccountId;
   }
   public String getUserName() {
      return userName;
   }
   public void setUserName(String userName) {
      this.userName = userName;
   }
   public String getAuthModel() {
      return authModel;
   }
   public void setAuthModel(String authModel) {
      this.authModel = authModel;
   }
   public long getExpires() {
      return expires;
   }
   public void setExpires(long expires) {
      this.expires = expires;
   }
   
}
