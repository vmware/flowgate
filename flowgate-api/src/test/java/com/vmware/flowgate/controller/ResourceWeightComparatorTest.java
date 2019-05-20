/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import com.vmware.flowgate.common.model.WormholeResources;
import com.vmware.flowgate.util.WormholeResourceWeightComparator;

import junit.framework.TestCase;

public class ResourceWeightComparatorTest {

   @Test
   public void testTreeMap() {
      WormholeResourceWeightComparator comparator = new WormholeResourceWeightComparator();
      TreeMap<WormholeResources, String> testTreemap =
            new TreeMap<WormholeResources, String>(comparator);
      
      WormholeResources resource = create();
      resource.setPattern("/v1/auth/user/page/**");
      resource.setSortWeight(5);
      testTreemap.put(resource, "getUserByPage");
      
      WormholeResources resource1 = create();
      resource1.setPattern("/v1/auth/user/**");
      resource1.setSortWeight(2);
      testTreemap.put(resource1, "getUsers");
      
      WormholeResources resource2 = create();
      resource2.setPattern("/v1/assets/mapping/vrops/**");
      resource2.setSortWeight(5);
      testTreemap.put(resource2, "getVropsMapping");
      
      WormholeResources resource3 = create();
      resource3.setPattern("/v1/assets/mapping/**");
      resource3.setSortWeight(2);
      testTreemap.put(resource3, "createMapping");
      
      Iterator<Map.Entry<WormholeResources, String>> iter = testTreemap.entrySet().iterator();
      ArrayList<WormholeResources> resources = new ArrayList<WormholeResources>();
      while (iter.hasNext()) {
         Map.Entry<WormholeResources, String> entry = iter.next();
         resources.add(entry.getKey());
      }
      
      TestCase.assertEquals(resources.get(0).getPattern(), "/v1/assets/mapping/vrops/**");
      TestCase.assertEquals(resources.get(1).getPattern(), "/v1/auth/user/page/**");
      TestCase.assertEquals(resources.get(2).getPattern(), "/v1/assets/mapping/**");
      TestCase.assertEquals(resources.get(3).getPattern(), "/v1/auth/user/**");
   }
   
   WormholeResources create() {
      return new WormholeResources();
   }
   
}
