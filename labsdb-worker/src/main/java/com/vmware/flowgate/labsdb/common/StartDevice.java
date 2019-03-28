/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.labsdb.common;

import java.util.List;

public class StartDevice {
   private String startDeviceName;
   private List<EndDevice> endDevices;

   public String getStartDeviceName() {
      return startDeviceName;
   }

   public void setStartDeviceName(String startDeviceName) {
      this.startDeviceName = startDeviceName;
   }

   public List<EndDevice> getEndDevices() {
      return endDevices;
   }

   public void setEndDevices(List<EndDevice> endDevices) {
      this.endDevices = endDevices;
   }

}
