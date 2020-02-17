/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class Parent {

   private ParentType type;
   private String parentId;

   public ParentType getType() {
      return type;
   }

   public void setType(ParentType type) {
      this.type = type;
   }

   public String getParentId() {
      return parentId;
   }

   public void setParentId(String parentId) {
      this.parentId = parentId;
   }

   public enum ParentType {
      PDU
   }
}
