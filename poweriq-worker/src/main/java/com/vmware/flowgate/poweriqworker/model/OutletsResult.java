/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import java.util.List;

public class OutletsResult {

   private List<Outlet> outlets;

   public List<Outlet> getOutlets() {
      return outlets;
   }

   public void setOutlets(List<Outlet> outlets) {
      this.outlets = outlets;
   }

}
