/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.io.Serializable;

import com.vmware.flowgate.common.RealtimeDataUnit;

public class AssetRealtimeDataSpec implements Serializable{

   private static final long serialVersionUID = 1L;
   private RealtimeDataUnit unit;
   private String validNumMin;
   private String validNumMax;

   public RealtimeDataUnit getUnit() {
      return unit;
   }

   public void setUnit(RealtimeDataUnit unit) {
      this.unit = unit;
   }

   public String getValidNumMin() {
      return validNumMin;
   }

   public void setValidNumMin(String validNumMin) {
      this.validNumMin = validNumMin;
   }

   public String getValidNumMax() {
      return validNumMax;
   }

   public void setValidNumMax(String validNumMax) {
      this.validNumMax = validNumMax;
   }
}
