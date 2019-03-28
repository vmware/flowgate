/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.io.Serializable;

import com.vmware.flowgate.common.Protocol;

public class AssetAddress implements Serializable{

   private static final long serialVersionUID = 1L;
   private Protocol protocol;
   private String address;
   private int port;
   private String user;
   private String password;

   public Protocol getProtocol() {
      return protocol;
   }

   public void setProtocol(Protocol protocol) {
      this.protocol = protocol;
   }

   public String getAddress() {
      return address;
   }

   public void setAddress(String address) {
      this.address = address;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public String getUser() {
      return user;
   }

   public void setUser(String user) {
      this.user = user;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

}
