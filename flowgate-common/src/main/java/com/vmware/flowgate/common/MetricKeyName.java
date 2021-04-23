/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common;

public class MetricKeyName {

   public static final String PDU_TEMPERATURE_LOCATIONX = "Temperature|:%s";
   public static final String PDU_HUMIDITY_LOCATIONX = "Humidity|:%s";

   public static final String SERVER_FRONT_TEMPERATURE_LOCATIONX = "FrontTemperature|%s";
   public static final String SERVER_BACK_TEMPREATURE_LOCATIONX = "BackTemperature|%s";
   public static final String SERVER_FRONT_HUMIDITY_LOCATIONX = "FrontHumidity|%s";
   public static final String SERVER_BACK_HUMIDITY_LOCATIONX = "BackHumidity|%s";
   public static final String SERVER_CONNECTED_PDUX_TOTAL_CURRENT = "PDU:%s|Current";
   public static final String SERVER_CONNECTED_PDUX_TOTAL_POWER = "PDU:%s|Power";
   public static final String SERVER_CONNECTED_PDUX_CURRENT_LOAD = "PDU:%s|CurrentLoad";
   public static final String SERVER_CONNECTED_PDUX_POWER_LOAD = "PDU:%s|PowerLoad";
   public static final String SERVER_CONNECTED_PDUX_OUTLETX_CURRENT = "PDU:%s|%s|Current";
   public static final String SERVER_CONNECTED_PDUX_OUTLETX_POWER = "PDU:%s|%s|Power";
   public static final String SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE = "PDU:%s|%s|Voltage";
}
