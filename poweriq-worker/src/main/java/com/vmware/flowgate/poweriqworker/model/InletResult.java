/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import java.util.List;

public class InletResult {

   private List<Inlet> inlets;

   public List<Inlet> getInlets() {
      return inlets;
   }

   public void setInlets(List<Inlet> inlets) {
      this.inlets = inlets;
   }

}
