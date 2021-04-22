/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.service;

import java.util.Comparator;

import com.vmware.flowgate.common.model.ValueUnit;

public class CompareValueUnitByTime implements Comparator<ValueUnit>{
   @Override
   public int compare(ValueUnit v1, ValueUnit v2) {
      if (v1.getTime() > v2.getTime()) {
         return 1;
      } else if (v1.getTime() == v2.getTime()) {
         return 0;
      }
      return -1;
   }
}
