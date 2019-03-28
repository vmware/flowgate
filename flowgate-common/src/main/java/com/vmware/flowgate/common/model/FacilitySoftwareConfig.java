/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.HashMap;

import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.security.EncryptionGuard;

public class FacilitySoftwareConfig implements Serializable {

   /**
    *
    */
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
   private HashMap<AdvanceSettingType,String> advanceSetting;

   public enum SoftwareType {
      Nlyte,PowerIQ,Device42,InfoBlox,OtherDCIM,OtherCMDB,Labsdb
   }

   public enum AdvanceSettingType{
      DateFormat,TimeZone,PDU_AMPS_UNIT, PDU_POWER_UNIT,PDU_VOLT_UNIT,TEMPERATURE_UNIT,HUMIDITY_UNIT
   }
   
   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public String getPassword() {
      if (password == null) {
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
      if (password == null || password.equals("")) {
         this.password = null;
      }else {
         try {
            this.password = EncryptionGuard.encode(password);
         } catch (UnsupportedEncodingException e) {
            throw new WormholeException(e.getMessage(), e.getCause());
         } catch (GeneralSecurityException e) {
            throw new WormholeException(e.getMessage(), e.getCause());
         }
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

   public HashMap<AdvanceSettingType, String> getAdvanceSetting() {
      return advanceSetting;
   }

   public void setAdvanceSetting(HashMap<AdvanceSettingType, String> advanceSetting) {
      this.advanceSetting = advanceSetting;
   }

   
}
