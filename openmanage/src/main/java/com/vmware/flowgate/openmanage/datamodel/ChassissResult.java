package com.vmware.flowgate.openmanage.datamodel;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChassissResult {

   @JsonProperty(value="@odata.context")
   private String odataContext;
   @JsonProperty(value="@odata.count")
   private int count;
   private List<Chassis> values;
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
   public List<Chassis> getValues() {
      return values;
   }
   public void setValues(List<Chassis> values) {
      this.values = values;
   }

}
