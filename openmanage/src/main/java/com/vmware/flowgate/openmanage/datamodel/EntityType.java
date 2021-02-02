/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public enum EntityType {

   Device(0),
   Group(1);
   private int value;

   private EntityType(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }

}
