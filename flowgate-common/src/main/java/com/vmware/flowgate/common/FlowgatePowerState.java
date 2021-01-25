/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common;

/**
 * References:DMTF-DSP1027 & ACPI
 * @author ppw
 *
 */
public enum FlowgatePowerState {
   ON,//System is fully on.(G0)
   SLEEP,//System is in Standby or Sleep state.(G1)
   OFFSOFT,//System is powered off where the system consumes a minimal amount of power.(G2)
   OFFHARD,//System is powered off except for the real- time clock, power consumption is zero.(G3)
   UNKNOWN;
}
