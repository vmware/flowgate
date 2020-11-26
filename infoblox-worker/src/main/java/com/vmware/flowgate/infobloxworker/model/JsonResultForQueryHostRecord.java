package com.vmware.flowgate.infobloxworker.model;

import java.util.List;

public class JsonResultForQueryHostRecord {

   private List<InfobloxHostRecordItem> result;

   public List<InfobloxHostRecordItem> getResult() {
      return result;
   }

   public void setResult(List<InfobloxHostRecordItem> result) {
      this.result = result;
   }

}
