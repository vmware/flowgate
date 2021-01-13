/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceManagement {

   @JsonProperty(value="ManagementId")
   private int managementId;
   @JsonProperty(value="NetworkAddress")
   private String networkAddress;
   @JsonProperty(value="MacAddress")
   private String macAddress;
   @JsonProperty(value="ManagementType")
   private int managementType;
   @JsonProperty(value="InstrumentationName")
   private String instrumentationName;
   @JsonProperty(value="DnsName")
   private String dnsName;
   @JsonProperty(value="ManagementProfile")
   private List<ManagementProfile> managementProfile;
   public int getManagementId() {
      return managementId;
   }
   public void setManagementId(int managementId) {
      this.managementId = managementId;
   }
   public String getNetworkAddress() {
      return networkAddress;
   }
   public void setNetworkAddress(String networkAddress) {
      this.networkAddress = networkAddress;
   }
   public String getMacAddress() {
      return macAddress;
   }
   public void setMacAddress(String macAddress) {
      this.macAddress = macAddress;
   }
   public int getManagementType() {
      return managementType;
   }
   public void setManagementType(int managementType) {
      this.managementType = managementType;
   }
   public String getInstrumentationName() {
      return instrumentationName;
   }
   public void setInstrumentationName(String instrumentationName) {
      this.instrumentationName = instrumentationName;
   }
   public String getDnsName() {
      return dnsName;
   }
   public void setDnsName(String dnsName) {
      this.dnsName = dnsName;
   }
   public List<ManagementProfile> getManagementProfile() {
      return managementProfile;
   }
   public void setManagementProfile(List<ManagementProfile> managementProfile) {
      this.managementProfile = managementProfile;
   }

}
