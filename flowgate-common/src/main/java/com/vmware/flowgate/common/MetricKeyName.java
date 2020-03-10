/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common;

public class MetricKeyName {

   public static final String PDU_XLET_ACTIVE_POWER = "%s|ActivePower";
   public static final String PDU_XLET_APPARENT_POWER = "%s|ApparentPower";
   public static final String PDU_XLET_CURRENT = "%s|Current";
   public static final String PDU_XLET_VOLTAGE = "%s|Voltage";
   public static final String PDU_XLET_FREE_CAPACITY = "%s|FreeCapacity";
   public static final String PDU_TEMPERATURE_LOCATIONX = "Temperature|:%s";
   public static final String PDU_HUMIDITY_LOCATIONX = "HUMIDITY|:%s";

   public static final String SERVER_FRONT_TEMPERATURE_LOCATIONX = "FrontTemperature|%s";
   public static final String SERVER_BACK_TEMPREATURE_LOCATIONX = "BackTemperature|%s";
   public static final String SERVER_FRONT_HUMIDITY_LOCATIONX = "FrontHumidity|%s";
   public static final String SERVER_BACK_HUMIDITY_LOCATIONX = "BackHumidity|%s";
   public static final String SERVER_CONNECTED_PDUX_TOTAL_CURRENT = "PDU:%s|Current";
   public static final String SERVER_CONNECTED_PDUX_TOTAL_POWER = "PDU:%s|Power";
   public static final String SERVER_CONNECTED_PDUX_OUTLETX_CURRENT = "PDU:%s|%s|Current";
   public static final String SERVER_CONNECTED_PDUX_OUTLETX_POWER = "PDU:%s|%s|Power";
   public static final String SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE = "PDU:%s|%s|Voltage";
}
