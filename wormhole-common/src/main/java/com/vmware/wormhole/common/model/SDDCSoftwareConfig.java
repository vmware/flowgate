/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.common.model;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import com.vmware.wormhole.common.exception.WormholeException;
import com.vmware.wormhole.common.security.EncryptionGuard;


public class SDDCSoftwareConfig implements Serializable {

   private static final long serialVersionUID = 1L;
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
      if(password == null) {
         return null;
      }
      try {
         return EncryptionGuard.decode(password);
      } catch (UnsupportedEncodingException e) {
         throw new WormholeException(e.getMessage(), e.getCause());
      } catch (GeneralSecurityException e) {
         throw new WormholeException(e.getMessage(), e.getCause());
      }
   }

   public void setPassword(String password) {
      if(password == null) {
         this.password = password;
      }
      try {
         this.password=EncryptionGuard.encode(password);
       } catch (UnsupportedEncodingException e) {
          throw new WormholeException(e.getMessage(), e.getCause());
       } catch (GeneralSecurityException e) {
          throw new WormholeException(e.getMessage(), e.getCause());
       }
   }

   public boolean checkIsActive() {
      if(this.integrationStatus != null && this.integrationStatus.getStatus()!=null &&
            !IntegrationStatus.Status.ACTIVE.equals(integrationStatus.getStatus())) {
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

