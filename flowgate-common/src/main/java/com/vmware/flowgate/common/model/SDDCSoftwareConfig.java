/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

public class SDDCSoftwareConfig implements Serializable, BaseDocument {

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
   private IntegrationStatus integrationStatus;
   private String subCategory;

   public enum SoftwareType {
      VRO, VCENTER, OTHERS, VROPSMP
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

   public String getSubCategory() {
      return subCategory;
   }

   public void setSubCategory(String subCategory) {
      this.subCategory = subCategory;
   }

}

