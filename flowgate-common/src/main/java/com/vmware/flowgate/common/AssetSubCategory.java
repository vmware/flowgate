/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common;

public enum AssetSubCategory {
   Blade(AssetCategory.Server), 
   Standard(AssetCategory.Server), 
   Humidity(AssetCategory.Sensors),
   Temperature(AssetCategory.Sensors), 
   AirPressure(AssetCategory.Sensors),
   AirFlow(AssetCategory.Sensors), 
   ContactClosure(AssetCategory.Sensors),
   Smoke(AssetCategory.Sensors),
   Water(AssetCategory.Sensors), 
   Vibration(AssetCategory.Sensors);
   private final AssetCategory parentCategory;

   AssetSubCategory(AssetCategory parent) {
      this.parentCategory = parent;
   }

   public AssetCategory getParentCategory() {
      return parentCategory;
   }
}
