package com.vmware.flowgate.openmanage.datamodel;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServersResult {

   @JsonProperty(value="@odata.context")
   private String odataContext;
   @JsonProperty(value="@odata.count")
   private int count;
   private List<Server> values;
   public String getOdataContext() {
      return odataContext;
   }
   public void setOdataContext(String odataContext) {
      this.odataContext = odataContext;
   }
   public int getCount() {
      return count;
   }
   public void setCount(int count) {
      this.count = count;
   }
   public List<Server> getValues() {
      return values;
   }
   public void setValues(List<Server> values) {
      this.values = values;
   }
}
