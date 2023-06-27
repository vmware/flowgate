/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.util.List;

import org.springframework.data.annotation.Id;

public class WormholeRole implements BaseDocument {
   /**
    * roleid
    */
   @Id
   private String id;
   /**
    * roleName
    */
   private String roleName;

   private List<String>  privilegeNames;

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getRoleName() {
      return roleName;
   }

   public void setRoleName(String roleName) {
      this.roleName = roleName;
   }

   public List<String> getPrivilegeNames() {
      return privilegeNames;
   }

   public void setPrivilegeNames(List<String> privilegeNames) {
      this.privilegeNames = privilegeNames;
   }

}
