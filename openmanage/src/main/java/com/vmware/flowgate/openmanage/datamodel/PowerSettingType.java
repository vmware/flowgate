/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

/**
 * The Id of PowerSetting
 * @author pengpengw
 *
 */
public enum PowerSettingType {

   TemperatureUnit(1),
   PowerUnit(2),
   MetricGatheringInterval(3),
   BuiltinReportTimeInterval(5),
   BuiltinReportTimeGranularity(6),
   TopEnergyConsumersDuration(7),
   DeleteMetricData(8),
   ResetWSMANPowerMetricdata(9);

   private int value;

   PowerSettingType(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }
}
