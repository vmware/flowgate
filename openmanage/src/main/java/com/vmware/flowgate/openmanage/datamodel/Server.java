/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Server extends Device{

   @JsonProperty(value="DeviceSpecificData")
   private ServerSpecificData deviceSpecificData;

   public ServerSpecificData getDeviceSpecificData() {
      return deviceSpecificData;
   }

   public void setDeviceSpecificData(ServerSpecificData deviceSpecificData) {
      this.deviceSpecificData = deviceSpecificData;
   }

}
