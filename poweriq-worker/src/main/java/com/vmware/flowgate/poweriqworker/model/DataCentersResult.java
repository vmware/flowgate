/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataCentersResult {

   @JsonProperty(value = "data_centers")
   private List<DataCenter> dataCenters;

   public List<DataCenter> getDataCenters() {
      return dataCenters;
   }

   public void setDataCenters(List<DataCenter> dataCenters) {
      this.dataCenters = dataCenters;
   }
   
   
}
