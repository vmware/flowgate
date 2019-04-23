/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import com.couchbase.client.java.repository.annotation.Id;
import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.security.EncryptionGuard;

import com.couchbase.client.java.repository.annotation.Id;

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

   public enum SoftwareType {
      VRO, VCENTER, OTHERS
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
      if (this.integrationStatus != null && this.integrationStatus.getStatus() != null
            && !IntegrationStatus.Status.ACTIVE.equals(integrationStatus.getStatus())) {
         return false;
      }
      return true;
   }

   public String getServerURL() {
      return serverURL;
   }

   public void setServerURL(String serverURL) {
      this.serverURL = serverURL;
   }

   public String getId() {
      return id;
   }

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

}

