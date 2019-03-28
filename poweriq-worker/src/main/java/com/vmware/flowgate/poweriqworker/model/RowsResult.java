/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import java.util.List;

public class RowsResult {

   private List<Row> rows;

   public List<Row> getRows() {
      return rows;
   }

   public void setRows(List<Row> rows) {
      this.rows = rows;
   }
   
   
}
