/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.common.model;

import java.io.Serializable;
import java.util.List;

public class DashBoardData implements Serializable {

   private static final long serialVersionUID = 1L;
   private int assetsNum = 0;
   private int facilitySystemNum = 0;
   private int categoryIsServerNum = 0;
   private int categoryIsPduNum = 0;
   private int categoryIsCabinetNum = 0;
   private int categoryIsSwitchNum = 0;
   private int categoryIsSensorNum = 0;
   private int categoryIsUpsNum = 0;
   private int subCategoryIsHumidity = 0;
   private int subCategoryIsTemperature = 0;
   private int subCategoryIsAirFlow = 0;


   private int subCategoryIsSmoke = 0;
   private int subCategoryIsWater = 0;
   private int userNum = 0;
   private int sddcServerNum = 0;
   private int sddcIntegrationNum = 0;
   private int sddcIntegrationVcNum = 0;
   private int sddcIntegrationVroNum = 0;
   private List<DashBoardSystemNlyteDetail> dashBoardSystemNlyteDetail;
   private List<DashBoardSystemPowerIqDetail> dashBoardSystemPowerIqDetail;
   private List<DashBoardSystemSddcServerVcDetail> dashBoardSystemSddcServerVcDetail;
   private List<DashBoardSystemSddcServerVroDetail> dashBoardSystemSddcServerVroDetail;

   public List<DashBoardSystemSddcServerVroDetail> getDashBoardSystemSddcServerVroDetail() {
      return dashBoardSystemSddcServerVroDetail;
   }

   public void setDashBoardSystemSddcServerVroDetail(
         List<DashBoardSystemSddcServerVroDetail> dashBoardSystemSddcServerVroDetail) {
      this.dashBoardSystemSddcServerVroDetail = dashBoardSystemSddcServerVroDetail;
   }

   public void setDashBoardSystemSddcServerVcDetail(
         List<DashBoardSystemSddcServerVcDetail> dashBoardSystemSddcServerVcDetail) {
      this.dashBoardSystemSddcServerVcDetail = dashBoardSystemSddcServerVcDetail;
   }

   public List<DashBoardSystemSddcServerVcDetail> getDashBoardSystemSddcServerVcDetail() {
      return dashBoardSystemSddcServerVcDetail;
   }

   public List<DashBoardSystemPowerIqDetail> getDashBoardSystemPowerIqDetail() {
      return dashBoardSystemPowerIqDetail;
   }

   public void setDashBoardSystemPowerIqDetail(
         List<DashBoardSystemPowerIqDetail> dashBoardSystemPowerIqDetail) {
      this.dashBoardSystemPowerIqDetail = dashBoardSystemPowerIqDetail;
   }

   public List<DashBoardSystemNlyteDetail> getDashBoardSystemNlyteDetail() {
      return dashBoardSystemNlyteDetail;
   }

   public void setDashBoardSystemNlyteDetail(
         List<DashBoardSystemNlyteDetail> dashBoardSystemNlyteDetail) {
      this.dashBoardSystemNlyteDetail = dashBoardSystemNlyteDetail;
   }

   public int getSubCategoryIsHumidity() {
      return subCategoryIsHumidity;
   }

   public void setSubCategoryIsHumidity(int subCategoryIsHumidity) {
      this.subCategoryIsHumidity = subCategoryIsHumidity;
   }

   public int getSubCategoryIsTemperature() {
      return subCategoryIsTemperature;
   }

   public void setSubCategoryIsTemperature(int subCategoryIsTemperature) {
      this.subCategoryIsTemperature = subCategoryIsTemperature;
   }

   public int getSubCategoryIsAirFlow() {
      return subCategoryIsAirFlow;
   }

   public void setSubCategoryIsAirFlow(int subCategoryIsAirFlow) {
      this.subCategoryIsAirFlow = subCategoryIsAirFlow;
   }

   public int getSubCategoryIsSmoke() {
      return subCategoryIsSmoke;
   }

   public void setSubCategoryIsSmoke(int subCategoryIsSmoke) {
      this.subCategoryIsSmoke = subCategoryIsSmoke;
   }

   public int getSubCategoryIsWater() {
      return subCategoryIsWater;
   }

   public void setSubCategoryIsWater(int subCategoryIsWater) {
      this.subCategoryIsWater = subCategoryIsWater;
   }

   public int getAssetsNum() {
      return assetsNum;
   }

   public void setAssetsNum(int assetsNum) {
      this.assetsNum = assetsNum;
   }

   public int getFacilitySystemNum() {
      return facilitySystemNum;
   }

   public void setFacilitySystemNum(int facilitySystemNum) {
      this.facilitySystemNum = facilitySystemNum;
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

   public int getUserNum() {
      return userNum;
   }

   public void setUserNum(int userNum) {
      this.userNum = userNum;
   }

   public int getSddcServerNum() {
      return sddcServerNum;
   }

   public void setSddcServerNum(int sddcServerNum) {
      this.sddcServerNum = sddcServerNum;
   }

   public int getSddcIntegrationNum() {
      return sddcIntegrationNum;
   }

   public void setSddcIntegrationNum(int sddcIntegrationNum) {
      this.sddcIntegrationNum = sddcIntegrationNum;
   }

   public int getSddcIntegrationVcNum() {
      return sddcIntegrationVcNum;
   }

   public void setSddcIntegrationVcNum(int sddcIntegrationVcNum) {
      this.sddcIntegrationVcNum = sddcIntegrationVcNum;
   }

   public int getSddcIntegrationVroNum() {
      return sddcIntegrationVroNum;
   }

   public void setSddcIntegrationVroNum(int sddcIntegrationVroNum) {
      this.sddcIntegrationVroNum = sddcIntegrationVroNum;
   }

}
