/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommonObject {

   @JsonProperty(value="@odata.id")
   private String link;

   public String getLink() {
      return link;
   }

   public void setLink(String link) {
      this.link = link;
   }

}
