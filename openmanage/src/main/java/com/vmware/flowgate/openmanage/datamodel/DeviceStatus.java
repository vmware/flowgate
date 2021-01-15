/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public enum DeviceStatus {
   NORMAL(1000),UNKNOWN(2000),WARNING(3000),CRITICAL(4000),NOSTATUS(5000);

   private int value;

   DeviceStatus(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }
}
