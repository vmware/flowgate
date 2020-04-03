package com.vmware.flowgate.common.model;

public class CabinetU {

   private String state;
   private int cabinetAssetID;
   private int cabinetUNumber;
   private String assetsOnU;
   public String getState() {
      return state;
   }
   public void setState(String state) {
      this.state = state;
   }
   public int getCabinetAssetID() {
      return cabinetAssetID;
   }
   public void setCabinetAssetID(int cabinetAssetID) {
      this.cabinetAssetID = cabinetAssetID;
   }
   public int getCabinetUNumber() {
      return cabinetUNumber;
   }
   public void setCabinetUNumber(int cabinetUNumber) {
      this.cabinetUNumber = cabinetUNumber;
   }
   public String getAssetsOnU() {
      return assetsOnU;
   }
   public void setAssetsOnU(String assetsOnU) {
      this.assetsOnU = assetsOnU;
   }

}
