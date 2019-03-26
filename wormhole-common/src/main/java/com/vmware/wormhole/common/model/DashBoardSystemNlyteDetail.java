/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.common.model;

public class DashBoardSystemNlyteDetail {
   private String nlyteName = "";
   private String nlyteUrl = "";
   private int categoryIsServerNum = 0;
   private int categoryIsPduNum = 0;
   private int categoryIsCabinetNum = 0;
   private int categoryIsSwitchNum = 0;
   private int categoryIsSensorNum = 0;
   private int categoryIsUpsNum = 0;

   public String getNlyteUrl() {
      return nlyteUrl;
   }

   public void setNlyteUrl(String nlyteUrl) {
      this.nlyteUrl = nlyteUrl;
   }

   public String getNlyteName() {
      return nlyteName;
   }

   public void setNlyteName(String nlyteName) {
      this.nlyteName = nlyteName;
   }

   public int getCategoryIsServerNum() {
      return categoryIsServerNum;
   }

   public void setCategoryIsServerNum(int categoryIsServerNum) {
      this.categoryIsServerNum = categoryIsServerNum;
   }

   public int getCategoryIsPduNum() {
      return categoryIsPduNum;
   }

   public void setCategoryIsPduNum(int categoryIsPduNum) {
      this.categoryIsPduNum = categoryIsPduNum;
   }

   public int getCategoryIsCabinetNum() {
      return categoryIsCabinetNum;
   }

   public void setCategoryIsCabinetNum(int categoryIsCabinetNum) {
      this.categoryIsCabinetNum = categoryIsCabinetNum;
   }

   public int getCategoryIsSwitchNum() {
      return categoryIsSwitchNum;
   }

   public void setCategoryIsSwitchNum(int categoryIsSwitchNum) {
      this.categoryIsSwitchNum = categoryIsSwitchNum;
   }

   public int getCategoryIsSensorNum() {
      return categoryIsSensorNum;
   }

   public void setCategoryIsSensorNum(int categoryIsSensorNum) {
      this.categoryIsSensorNum = categoryIsSensorNum;
   }

   public int getCategoryIsUpsNum() {
      return categoryIsUpsNum;
   }

   public void setCategoryIsUpsNum(int categoryIsUpsNum) {
      this.categoryIsUpsNum = categoryIsUpsNum;
   }

}
