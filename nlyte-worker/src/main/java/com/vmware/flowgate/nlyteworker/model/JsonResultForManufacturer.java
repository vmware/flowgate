/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonResultForManufacturer {

   @JsonProperty(value="@odata.context")
   private String  odatacontext;
   
   private List<Manufacturer> value;
   
   @JsonProperty(value="@odata.nextLink")
   private String odatanextLink;

   public String getOdatacontext() {
      return odatacontext;
   }

   public void setOdatacontext(String odatacontext) {
      this.odatacontext = odatacontext;
   }

   public String getOdatanextLink() {
      return odatanextLink;
   }

   public void setOdatanextLink(String odatanextLink) {
      this.odatanextLink = odatanextLink;
   }

   public List<Manufacturer> getValue() {
      return value;
   }

   public void setValue(List<Manufacturer> value) {
      this.value = value;
   }
   
}
