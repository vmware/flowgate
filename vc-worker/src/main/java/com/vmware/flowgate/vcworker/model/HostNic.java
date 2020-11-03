package com.vmware.flowgate.vcworker.model;

public class HostNic {

   private String name; // name of nic
   private String macAddress; // mac address
   private int linkSpeedMb; // eg. 1000MB
   private boolean duplex; // whether or not duplex
   private String driver; // eg. "ntg3"

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getMacAddress() {
      return macAddress;
   }

   public void setMacAddress(String macAddress) {
      this.macAddress = macAddress;
   }

   public int getLinkSpeedMb() {
      return linkSpeedMb;
   }

   public void setLinkSpeedMb(int linkSpeedMb) {
      this.linkSpeedMb = linkSpeedMb;
   }

   public boolean isDuplex() {
      return duplex;
   }

   public void setDuplex(boolean duplex) {
      this.duplex = duplex;
   }

   public String getDriver() {
      return driver;
   }

   public void setDriver(String driver) {
      this.driver = driver;
   }

}
