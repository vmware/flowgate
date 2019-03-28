/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Manufacturer {
   @JsonProperty(value = "ManufacturerID")
   private int manufacturerID;
   @JsonProperty(value = "Detail")
   private String detail;
   public int getManufacturerID() {
      return manufacturerID;
   }
   public void setManufacturerID(int manufacturerID) {
      this.manufacturerID = manufacturerID;
   }
   public String getDetail() {
      return detail;
   }
   public void setDetail(String detail) {
      this.detail = detail;
   }
   
}
