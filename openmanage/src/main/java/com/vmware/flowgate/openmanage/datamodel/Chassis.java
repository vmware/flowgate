/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Chassis extends Device{

   @JsonProperty(value="DeviceSpecificData")
   private ChassisSpecificData deviceSpecificData;

   public ChassisSpecificData getDeviceSpecificData() {
      return deviceSpecificData;
   }

   public void setDeviceSpecificData(ChassisSpecificData deviceSpecificData) {
      this.deviceSpecificData = deviceSpecificData;
   }

}
