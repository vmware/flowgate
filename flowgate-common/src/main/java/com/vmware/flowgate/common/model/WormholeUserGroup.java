/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.util.List;

public class WormholeUserGroup {

   private String id;
   private String groupName;
   private List<WormholePrivilege> privileges;
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   public String getGroupName() {
      return groupName;
   }
   public void setGroupName(String groupName) {
      this.groupName = groupName;
   }
   public List<WormholePrivilege> getPrivileges() {
      return privileges;
   }
   public void setPrivileges(List<WormholePrivilege> privileges) {
      this.privileges = privileges;
   }
   
}
