package com.vmware.flowgate.infobloxworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiscoveredData {

   @JsonProperty(value = "first_discovered")
   private long firstDiscovered;
   @JsonProperty(value = "last_discovered")
   private long lastDiscovered;
   @JsonProperty(value = "mac_address")
   private String macAddress;
   @JsonProperty(value = "os")
   private String os;

   public long getFirstDiscovered() {
      return firstDiscovered;
   }
   public long getLastDiscovered() {
      return lastDiscovered;
   }
   public String getMacAddress() {
      return macAddress;
   }
   public String getOs() {
      return os;
   }
   public void setFirstDiscovered(long firstDiscovered) {
      this.firstDiscovered = firstDiscovered;
   }
   public void setLastDiscovered(long lastDiscovered) {
      this.lastDiscovered = lastDiscovered;
   }
   public void setMacAddress(String macAddress) {
      this.macAddress = macAddress;
   }
   public void setOs(String os) {
      this.os = os;
   }

}
