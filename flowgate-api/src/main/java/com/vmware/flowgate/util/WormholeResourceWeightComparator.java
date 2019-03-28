/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.util;

import java.util.Comparator;

import com.vmware.flowgate.common.model.WormholeResources;

public class WormholeResourceWeightComparator implements Comparator{

   @Override
   public int compare(Object o1, Object o2) {
      // TODO Auto-generated method stub
      WormholeResources resources = (WormholeResources)o1;
      WormholeResources resources2 = (WormholeResources)o2;
      if(resources2.getSortWeight()-resources.getSortWeight() == 0) {
         return -1;
      }
      return resources2.getSortWeight()-resources.getSortWeight();
   }

}
