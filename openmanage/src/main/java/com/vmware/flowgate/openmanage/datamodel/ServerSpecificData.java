/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public class ServerSpecificData {

   private boolean sdCardPresent;
   private String idracDnsName;
   private int vlanId;
   private int maxDIMMSlots;
   private int populatedDIMMSlots;
   private String manufacturer;
   private int populatedCPUSockets;
   private int totalMemory;
   private String chassisSlot;
   private int populatedPCISlots;
   private String serviceTag;
   private String chassisServiceTag;
   private String  platformGuid;
   private int maxCPUSockets;
   private String  expressServiceCode;
   private String nodeId;
   private int maxPCISlots;
   public boolean isSdCardPresent() {
      return sdCardPresent;
   }
   public void setSdCardPresent(boolean sdCardPresent) {
      this.sdCardPresent = sdCardPresent;
   }
   public String getIdracDnsName() {
      return idracDnsName;
   }
   public void setIdracDnsName(String idracDnsName) {
      this.idracDnsName = idracDnsName;
   }
   public int getVlanId() {
      return vlanId;
   }
   public void setVlanId(int vlanId) {
      this.vlanId = vlanId;
   }
   public int getMaxDIMMSlots() {
      return maxDIMMSlots;
   }
   public void setMaxDIMMSlots(int maxDIMMSlots) {
      this.maxDIMMSlots = maxDIMMSlots;
   }
   public int getPopulatedDIMMSlots() {
      return populatedDIMMSlots;
   }
   public void setPopulatedDIMMSlots(int populatedDIMMSlots) {
      this.populatedDIMMSlots = populatedDIMMSlots;
   }
   public String getManufacturer() {
      return manufacturer;
   }
   public void setManufacturer(String manufacturer) {
      this.manufacturer = manufacturer;
   }
   public int getPopulatedCPUSockets() {
      return populatedCPUSockets;
   }
   public void setPopulatedCPUSockets(int populatedCPUSockets) {
      this.populatedCPUSockets = populatedCPUSockets;
   }
   public int getTotalMemory() {
      return totalMemory;
   }
   public void setTotalMemory(int totalMemory) {
      this.totalMemory = totalMemory;
   }
   public String getChassisSlot() {
      return chassisSlot;
   }
   public void setChassisSlot(String chassisSlot) {
      this.chassisSlot = chassisSlot;
   }
   public int getPopulatedPCISlots() {
      return populatedPCISlots;
   }
   public void setPopulatedPCISlots(int populatedPCISlots) {
      this.populatedPCISlots = populatedPCISlots;
   }
   public String getServiceTag() {
      return serviceTag;
   }
   public void setServiceTag(String serviceTag) {
      this.serviceTag = serviceTag;
   }
   public String getChassisServiceTag() {
      return chassisServiceTag;
   }
   public void setChassisServiceTag(String chassisServiceTag) {
      this.chassisServiceTag = chassisServiceTag;
   }
   public String getPlatformGuid() {
      return platformGuid;
   }
   public void setPlatformGuid(String platformGuid) {
      this.platformGuid = platformGuid;
   }
   public int getMaxCPUSockets() {
      return maxCPUSockets;
   }
   public void setMaxCPUSockets(int maxCPUSockets) {
      this.maxCPUSockets = maxCPUSockets;
   }
   public String getExpressServiceCode() {
      return expressServiceCode;
   }
   public void setExpressServiceCode(String expressServiceCode) {
      this.expressServiceCode = expressServiceCode;
   }
   public String getNodeId() {
      return nodeId;
   }
   public void setNodeId(String nodeId) {
      this.nodeId = nodeId;
   }
   public int getMaxPCISlots() {
      return maxPCISlots;
   }
   public void setMaxPCISlots(int maxPCISlots) {
      this.maxPCISlots = maxPCISlots;
   }

}
