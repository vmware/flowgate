/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public enum PowerDisplayUnit {

   Watt(1),
   BTUPerHr(2);
   private int value;

   PowerDisplayUnit(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }
}
