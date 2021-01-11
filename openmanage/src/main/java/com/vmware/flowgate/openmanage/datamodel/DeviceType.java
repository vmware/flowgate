/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public enum DeviceType {

   SERVER(1000),CHASSIS(2000),STORAGE(3000),NETWORK_IOM(4000),
   DELLSTORAGE(5000),NETWORKSWITCH(7000),STORAGE_IOM(8000),NETWORK_CONTROLLER(9000);
   private int value;

   DeviceType(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }

}
