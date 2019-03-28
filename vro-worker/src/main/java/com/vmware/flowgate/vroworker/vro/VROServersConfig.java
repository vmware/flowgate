/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker.vro;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class VROServersConfig {
   private List<VROConfig> vroServers;

   public List<VROConfig> getVroServers() {
      if (vroServers == null) {
         vroServers = new ArrayList<VROConfig>();
      }
      return vroServers;
   }

   public void setVroServers(List<VROConfig> vroServers) {
      this.vroServers = vroServers;
   }


}
