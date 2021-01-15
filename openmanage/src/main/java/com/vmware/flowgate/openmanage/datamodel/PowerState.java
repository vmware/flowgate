/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public enum PowerState {

   UNKNOWN(1),ON(17),OFF(18),POWERINGON(20),POWERINGOFF(21);
   private int value;

   PowerState(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }
}
