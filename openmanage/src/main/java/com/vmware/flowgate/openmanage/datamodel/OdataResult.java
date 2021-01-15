/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OdataResult {

   @JsonProperty(value="@odata.context")
   private String context;
   @JsonProperty(value="@odata.count")
   private int count;
   @JsonProperty(value="@odata.nextLink")
   private String nextLink;
   public String getContext() {
      return context;
   }
   public void setContext(String context) {
      this.context = context;
   }
   public int getCount() {
      return count;
   }
   public void setCount(int count) {
      this.count = count;
   }
   public String getNextLink() {
      return nextLink;
   }
   public void setNextLink(String nextLink) {
      this.nextLink = nextLink;
   }

}
