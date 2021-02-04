/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public enum TemperatureDisplayUnit {

   Celsius(1),
   Fahrenheit(2);
   private int value;

   TemperatureDisplayUnit(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }
}
