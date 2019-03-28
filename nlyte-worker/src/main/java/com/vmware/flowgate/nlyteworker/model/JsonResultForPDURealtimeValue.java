/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonResultForPDURealtimeValue {

   @JsonProperty(value="@odata.context")
   private String  odatacontext;
   
   private List<PowerStripsRealtimeValue> value;

   public String getOdatacontext() {
      return odatacontext;
   }

   public void setOdatacontext(String odatacontext) {
      this.odatacontext = odatacontext;
   }

   public List<PowerStripsRealtimeValue> getValue() {
      return value;
   }

   public void setValue(List<PowerStripsRealtimeValue> value) {
      this.value = value;
   }
   
   
}
