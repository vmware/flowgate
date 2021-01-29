/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public enum Duration {

   Recent(0),
   OneHour(1),
   SixHours(2),
   TwelveHours(3),
   OneDay(4),
   SevenDays(5),
   OneMonth(6),
   ThreeMonths(7),
   SixMonths(8),
   OneYear(9);
   private int value;

   private Duration(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }

}
