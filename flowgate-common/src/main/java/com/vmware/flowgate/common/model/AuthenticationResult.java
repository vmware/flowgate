/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.util.Set;

public class AuthenticationResult {
   private String userName;
   private AuthToken token;
   private Set<String> privileges;
   
   public String getUserName() {
      return userName;
   }
   public void setUserName(String userName) {
      this.userName = userName;
   }
   public AuthToken getToken() {
      return token;
   }
   public void setToken(AuthToken token) {
      this.token = token;
   }
   public Set<String> getPrivileges() {
      return privileges;
   }
   public void setPrivileges(Set<String> privileges) {
      this.privileges = privileges;
   }
   
   
}
