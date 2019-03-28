/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import java.util.List;

public class PdusResult {

   private List<Pdu> pdus;

   public List<Pdu> getPdus() {
      return pdus;
   }

   public void setPdus(List<Pdu> pdus) {
      this.pdus = pdus;
   }
   
   
}
