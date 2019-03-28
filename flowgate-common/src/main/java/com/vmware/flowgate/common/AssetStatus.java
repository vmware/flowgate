package com.vmware.flowgate.common;

public class AssetStatus {

   private Status status;
   private PduMapping pduMapping;
   private NetworkMapping networkMapping;
   public Status getStatus() {
      return status;
   }
   public void setStatus(Status status) {
      this.status = status;
   }
   public PduMapping getPduMapping() {
      return pduMapping;
   }
   public void setPduMapping(PduMapping pduMapping) {
      this.pduMapping = pduMapping;
   }
   public NetworkMapping getNetworkMapping() {
      return networkMapping;
   }
   public void setNetworkMapping(NetworkMapping networkMapping) {
      this.networkMapping = networkMapping;
   }
   public enum Status {
      Planned,Active,Recycled,Decommissioned,Cancelled
   }
   
}
