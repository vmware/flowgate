/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import java.util.List;

public class SensorsResult {

   private List<Sensor> sensors;

   public List<Sensor> getSensors() {
      return sensors;
   }

   public void setSensors(List<Sensor> sensors) {
      this.sensors = sensors;
   }
   
   
}
