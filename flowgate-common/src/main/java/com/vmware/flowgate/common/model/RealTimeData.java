/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.util.List;

import org.springframework.data.annotation.Id;

public class RealTimeData implements BaseDocument {
   @Id
   private String id;
   private String assetID;

   private List<ValueUnit> values;

   private long time;

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getAssetID() {
      return assetID;
   }

   public void setAssetID(String assetID) {
      this.assetID = assetID;
   }

   public List<ValueUnit> getValues() {
      return values;
   }

   public void setValues(List<ValueUnit> values) {
      this.values = values;
   }

   public long getTime() {
      return time;
   }

   public void setTime(long time) {
      this.time = time;
   }
}
