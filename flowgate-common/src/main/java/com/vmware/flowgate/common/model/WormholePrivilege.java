/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;


import com.vmware.flowgate.common.PrivilegeType;

public class WormholePrivilege {
   private String id;
   private String name;
   private PrivilegeType type;
   
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public PrivilegeType getType() {
      return type;
   }
   public void setType(PrivilegeType type) {
      this.type = type;
   }
   
}
