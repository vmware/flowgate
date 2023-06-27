/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.io.Serializable;
import java.util.HashMap;

import org.springframework.data.annotation.Id;

public class FacilitySoftwareConfig implements Serializable, BaseDocument {

   /**
    *
    */
   private static final long serialVersionUID = 1L;
   @Id
   private String id;
   private String name;
   private String description;
   private String userName;
   private String password;
   private String serverURL;
   private SoftwareType type;
   private String userId;
   private boolean verifyCert;
   private String subCategory;
   private IntegrationStatus integrationStatus;
   private HashMap<AdvanceSettingType, String> advanceSetting;

   public enum SoftwareType {
      Nlyte, PowerIQ, Device42, InfoBlox, OpenManage, OtherDCIM, OtherCMDB, Labsdb
   }

   public enum AdvanceSettingType {
      DateFormat, TimeZone, PDU_AMPS_UNIT, PDU_POWER_UNIT, PDU_VOLT_UNIT,
      TEMPERATURE_UNIT, HUMIDITY_UNIT, INFOBLOX_PROXY_SEARCH
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public boolean checkIsActive() {
      if (this.integrationStatus != null && this.integrationStatus.getStatus() != null) {
         return IntegrationStatus.Status.ACTIVE.equals(integrationStatus.getStatus()) || IntegrationStatus.Status.WARNING.equals(integrationStatus.getStatus());
      }
      return true;
   }

   public String getServerURL() {
      return serverURL;
   }

   public void setServerURL(String serverURL) {
      this.serverURL = serverURL;
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public void setId(String id) {
      this.id = id;
   }

   public SoftwareType getType() {
      return type;
   }

   public void setType(SoftwareType type) {
      this.type = type;
   }

   public String getUserId() {
      return userId;
   }

   public void setUserId(String userId) {
      this.userId = userId;
   }

   public boolean isVerifyCert() {
      return verifyCert;
   }

   public void setVerifyCert(boolean verifyCert) {
      this.verifyCert = verifyCert;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public IntegrationStatus getIntegrationStatus() {
      return integrationStatus;
   }

   public void setIntegrationStatus(IntegrationStatus integrationStatus) {
      this.integrationStatus = integrationStatus;
   }

   public HashMap<AdvanceSettingType, String> getAdvanceSetting() {
      return advanceSetting;
   }

   public void setAdvanceSetting(HashMap<AdvanceSettingType, String> advanceSetting) {
      this.advanceSetting = advanceSetting;
   }

   public String getSubCategory() {
      return subCategory;
   }

   public void setSubCategory(String subCategory) {
      this.subCategory = subCategory;
   }

}
