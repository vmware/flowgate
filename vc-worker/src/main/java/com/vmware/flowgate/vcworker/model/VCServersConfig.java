/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vcworker.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.vmware.flowgate.common.model.SDDCSoftwareConfig;

@Service
public class VCServersConfig {
   private List<SDDCSoftwareConfig> vCs;

   public List<SDDCSoftwareConfig> getVCs() {
      if(vCs ==null) {
         vCs = new ArrayList<SDDCSoftwareConfig>();
      }
      return vCs;
   }

   public void setVCs(List<SDDCSoftwareConfig> vCs) {
      this.vCs = vCs;
   }

}
