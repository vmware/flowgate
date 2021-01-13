/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ManagementProfile {

   @JsonProperty(value="ManagementProfileId")
   private int managementProfileId;
   @JsonProperty(value="ProfileId")
   private String profileId;
   @JsonProperty(value="ManagementId")
   private int ManagementId;
   @JsonProperty(value="AgentName")
   private String agentName;
   @JsonProperty(value="ManagementURL")
   private String ManagementURL;
   @JsonProperty(value="HasCreds")
   private int HasCreds;
   @JsonProperty(value="Status")
   private int Status;
   @JsonProperty(value="StatusDateTime")
   private String StatusDateTime;

   public int getManagementProfileId() {
      return managementProfileId;
   }
   public void setManagementProfileId(int managementProfileId) {
      this.managementProfileId = managementProfileId;
   }
   public String getProfileId() {
      return profileId;
   }
   public void setProfileId(String profileId) {
      this.profileId = profileId;
   }
   public int getManagementId() {
      return ManagementId;
   }
   public void setManagementId(int managementId) {
      ManagementId = managementId;
   }
   public String getAgentName() {
      return agentName;
   }
   public void setAgentName(String agentName) {
      this.agentName = agentName;
   }
   public String getManagementURL() {
      return ManagementURL;
   }
   public void setManagementURL(String managementURL) {
      ManagementURL = managementURL;
   }
   public int getHasCreds() {
      return HasCreds;
   }
   public void setHasCreds(int hasCreds) {
      HasCreds = hasCreds;
   }
   public int getStatus() {
      return Status;
   }
   public void setStatus(int status) {
      Status = status;
   }
   public String getStatusDateTime() {
      return StatusDateTime;
   }
   public void setStatusDateTime(String statusDateTime) {
      StatusDateTime = statusDateTime;
   }

}
