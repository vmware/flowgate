/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class Parent {

   private String type;
   private String parentId;

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getParentId() {
      return parentId;
   }

   public void setParentId(String parentId) {
      this.parentId = parentId;
   }

}
