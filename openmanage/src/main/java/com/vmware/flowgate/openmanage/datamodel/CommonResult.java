/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import java.util.List;

public class CommonResult<T> extends OdataResult{

   private List<T> value;
   public List<T> getValue() {
      return value;
   }
   public void setValue(List<T> value) {
      this.value = value;
   }

}
