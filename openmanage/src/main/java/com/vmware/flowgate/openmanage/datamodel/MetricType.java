/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public enum MetricType {

   MAX_POWER(1),
   MIN_POWER(2),
   AVG_POWER(3),
   INSTANT_POWER(4),
   MAX_INLET_TEMP(5),
   MIN_INLET_TEMP(6),
   AVG_INLET_TEMP(7),
   INSTANT_TEMP(8),
   MAX_UTIL_CPU(9),
   MIN_UTIL_CPU(10),
   AVG_UTIL_CPU(11),
   MAX_UTIL_MEM(12),
   MIN_UTIL_MEM(13),
   AVG_UTIL_MEM(14),
   MAX_UTIL_IO(15),
   MIN_UTIL_IO(16),
   AVG_UTIL_IO(17),
   SYS_AIRFLOW(18);
   private int value;

   private MetricType(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }

}
