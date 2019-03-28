/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import java.util.List;

public class RacksResult {

   private List<Rack> racks;

   public List<Rack> getRacks() {
      return racks;
   }

   public void setRacks(List<Rack> racks) {
      this.racks = racks;
   }
   
   
}
