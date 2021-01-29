/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public enum SortOrder {

   Descending(0),
   Ascending(1);
   private int value;

   private SortOrder(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }

}
