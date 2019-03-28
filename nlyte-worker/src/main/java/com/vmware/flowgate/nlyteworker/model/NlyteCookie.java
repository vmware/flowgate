/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;

public class NlyteCookie {

   private String token;
   private String domain;
   private long expired_time;
   public String getToken() {
      return token;
   }
   public void setToken(String token) {
      this.token = token;
   }
   public String getDomain() {
      return domain;
   }
   public void setDomain(String domain) {
      this.domain = domain;
   }
   public long getExpired_time() {
      return expired_time;
   }
   public void setExpired_time(long expired_time) {
      this.expired_time = expired_time;
   }
   
}
