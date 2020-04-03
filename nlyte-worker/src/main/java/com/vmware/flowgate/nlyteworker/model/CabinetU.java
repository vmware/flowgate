/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CabinetU {

   @JsonProperty(value = "CabinetUState")
   private String cabinetUState;
   @JsonProperty(value = "CabinetAssetID")
   private int cabinetAssetID;
   @JsonProperty(value = "CabinetUNumber")
   private int cabinetUNumber;
   @JsonProperty(value = "AssetsOnU")
   private String assetsOnU;
   public String getCabinetUState() {
      return cabinetUState;
   }
   public void setCabinetUState(String cabinetUState) {
      this.cabinetUState = cabinetUState;
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
