package com.vmware.flowgate.common;

public enum PduMapping {
   UNMAPPED(0),
   MAPPEDBYAGGREGATOR(1),
   MAPPEDBYLABSDB(2);
   
   private int weight;
   private PduMapping(int weight) {
      this.weight = weight;
   }
   public int getWeight() {
      return weight;
   }
   public void setWeight(int weight) {
      this.weight = weight;
   }
   
}
