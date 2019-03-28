/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import java.util.List;

public class AislesResult {

   private List<Aisle> aisles;

   public List<Aisle> getAisles() {
      return aisles;
   }

   public void setAisles(List<Aisle> aisles) {
      this.aisles = aisles;
   }
   
   
}
