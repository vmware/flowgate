/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UMounting {
   @JsonProperty(value="MountingSide")
   private String mountingSide;
   @JsonProperty(value="MountingPlacement")
   private String mountingPlacement;
   @JsonProperty(value="MountingRotation")
   private String mountingRotation;
   @JsonProperty(value="UMountingID")
   private long uMountingID;
   @JsonProperty(value="CabinetAssetID")
   private long cabinetAssetID;
   @JsonProperty(value="CabinetUNumber")
   private int cabinetUNumber;
   @JsonProperty(value="UDisplayLabel")
   private int uDisplayLabel;
   @JsonProperty(value="LowerCabinetU")
   private int lowerCabinetU;
   @JsonProperty(value="XOffset")
   private int xOffset;
   @JsonProperty(value="YOffset")
   private int yOffset;
   @JsonProperty(value="ZOffset")
   private int zOffset;
   @JsonProperty(value="XRotation")
   private int xRotation;
   @JsonProperty(value="YRotation")
   private int yRotation;
   @JsonProperty(value="ZRotation")
   private int ZRotation;
   public String getMountingSide() {
      return mountingSide;
   }
   public void setMountingSide(String mountingSide) {
      this.mountingSide = mountingSide;
   }
   public String getMountingPlacement() {
      return mountingPlacement;
   }
   public void setMountingPlacement(String mountingPlacement) {
      this.mountingPlacement = mountingPlacement;
   }
   public String getMountingRotation() {
      return mountingRotation;
   }
   public void setMountingRotation(String mountingRotation) {
      this.mountingRotation = mountingRotation;
   }
   public long getuMountingID() {
      return uMountingID;
   }
   public void setuMountingID(long uMountingID) {
      this.uMountingID = uMountingID;
   }
   public long getCabinetAssetID() {
      return cabinetAssetID;
   }
   public void setCabinetAssetID(long cabinetAssetID) {
      this.cabinetAssetID = cabinetAssetID;
   }
   public int getCabinetUNumber() {
      return cabinetUNumber;
   }
   public void setCabinetUNumber(int cabinetUNumber) {
      this.cabinetUNumber = cabinetUNumber;
   }
   public int getuDisplayLabel() {
      return uDisplayLabel;
   }
   public void setuDisplayLabel(int uDisplayLabel) {
      this.uDisplayLabel = uDisplayLabel;
   }
   public int getLowerCabinetU() {
      return lowerCabinetU;
   }
   public void setLowerCabinetU(int lowerCabinetU) {
      this.lowerCabinetU = lowerCabinetU;
   }
   public int getxOffset() {
      return xOffset;
   }
   public void setxOffset(int xOffset) {
      this.xOffset = xOffset;
   }
   public int getyOffset() {
      return yOffset;
   }
   public void setyOffset(int yOffset) {
      this.yOffset = yOffset;
   }
   public int getzOffset() {
      return zOffset;
   }
   public void setzOffset(int zOffset) {
      this.zOffset = zOffset;
   }
   public int getxRotation() {
      return xRotation;
   }
   public void setxRotation(int xRotation) {
      this.xRotation = xRotation;
   }
   public int getyRotation() {
      return yRotation;
   }
   public void setyRotation(int yRotation) {
      this.yRotation = yRotation;
   }
   public int getZRotation() {
      return ZRotation;
   }
   public void setZRotation(int zRotation) {
      ZRotation = zRotation;
   }
   
   
}
