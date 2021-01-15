/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Device {
   @JsonProperty(value="Id")
   private long id;
   @JsonProperty(value="Type")
   private int type;
   @JsonProperty(value="Identifier")
   private String identifier;
   @JsonProperty(value="DeviceServiceTag")
   private String deviceServiceTag;
   @JsonProperty(value="ChassisServiceTag")
   private String chassisServiceTag;
   @JsonProperty(value="Model")
   private String model;
   @JsonProperty(value="PowerState")
   private int powerState;
   @JsonProperty(value="ManagedState")
   private int manageState;
   @JsonProperty(value="Status")
   private int status;
   @JsonProperty(value="ConnectionState")
   private boolean connectionState;
   @JsonProperty(value="AssetTag")
   private String assetTag;
   @JsonProperty(value="SystemId")
   private int systemId;
   @JsonProperty(value="DeviceName")
   private String deviceName;
   @JsonProperty(value="LastInventoryTime")
   private String lastInventoryTime;
   @JsonProperty(value="LastStatusTime")
   private String lastStatusTime;
   @JsonProperty(value="DeviceSubscription")
   private String deviceSubscription;
   @JsonProperty(value="DeviceCapabilities")
   private int[] DeviceCapabilities;
   @JsonProperty(value="SlotConfiguration")
   private SlotConfiguration slotConfiguration;
   @JsonProperty(value="DeviceManagement")
   private List<DeviceManagement> deviceManagement;
   @JsonProperty(value="Actions")
   private String actions;
   @JsonProperty(value="SensorHealth@odata.navigationLink")
   private String sensorHealthLink;
   @JsonProperty(value="VirtualSession")
   private CommonObject virtualSession;
   @JsonProperty(value="Baselines")
   private CommonObject baselines;
   @JsonProperty(value="InventoryDetails@odata.navigationLink")
   private String inventoryDetailsLink;
   @JsonProperty(value="HardwareLogs@odata.navigationLink")
   private String hardwareLogsLink;
   @JsonProperty(value="SubSystemHealth@odata.navigationLink")
   private String subSystemHealthLink;
   @JsonProperty(value="RecentActivity@odata.navigationLink")
   private String recentActivityLink;
   @JsonProperty(value="InventoryTypes")
   private CommonObject inventoryTypes;
   @JsonProperty(value="LogSeverities")
   private CommonObject logSeverities;
   @JsonProperty(value="Settings@odata.navigationLink")
   private String settingLink;
   @JsonProperty(value="Temperature")
   private CommonObject temperature;
   @JsonProperty(value="Power")
   private CommonObject power;
   @JsonProperty(value="SystemUpTime")
   private CommonObject systemUpTime;
   @JsonProperty(value="BlinkStatus")
   private CommonObject blinkStatus;
   @JsonProperty(value="PowerUsageByDevice@odata.navigationLink")
   private String powerUsageByDeviceLink;
   @JsonProperty(value="DeviceBladeSlots@odata.navigationLink")
   private String deviceBladeSlotsLink;
   @JsonProperty(value="GraphicInfo")
   private CommonObject graphicInfo;
   @JsonProperty(value="DeployRequired")
   private CommonObject deployRequired;
   public long getId() {
      return id;
   }
   public void setId(long id) {
      this.id = id;
   }
   public int getType() {
      return type;
   }
   public void setType(int type) {
      this.type = type;
   }
   public String getIdentifier() {
      return identifier;
   }
   public void setIdentifier(String identifier) {
      this.identifier = identifier;
   }
   public String getDeviceServiceTag() {
      return deviceServiceTag;
   }
   public void setDeviceServiceTag(String deviceServiceTag) {
      this.deviceServiceTag = deviceServiceTag;
   }
   public String getChassisServiceTag() {
      return chassisServiceTag;
   }
   public void setChassisServiceTag(String chassisServiceTag) {
      this.chassisServiceTag = chassisServiceTag;
   }
   public String getModel() {
      return model;
   }
   public void setModel(String model) {
      this.model = model;
   }
   public int getPowerState() {
      return powerState;
   }
   public void setPowerState(int powerState) {
      this.powerState = powerState;
   }
   public int getManageState() {
      return manageState;
   }
   public void setManageState(int manageState) {
      this.manageState = manageState;
   }
   public int getStatus() {
      return status;
   }
   public void setStatus(int status) {
      this.status = status;
   }
   public boolean isConnectionState() {
      return connectionState;
   }
   public void setConnectionState(boolean connectionState) {
      this.connectionState = connectionState;
   }
   public String getAssetTag() {
      return assetTag;
   }
   public void setAssetTag(String assetTag) {
      this.assetTag = assetTag;
   }
   public int getSystemId() {
      return systemId;
   }
   public void setSystemId(int systemId) {
      this.systemId = systemId;
   }
   public String getDeviceName() {
      return deviceName;
   }
   public void setDeviceName(String deviceName) {
      this.deviceName = deviceName;
   }
   public String getLastInventoryTime() {
      return lastInventoryTime;
   }
   public void setLastInventoryTime(String lastInventoryTime) {
      this.lastInventoryTime = lastInventoryTime;
   }
   public String getLastStatusTime() {
      return lastStatusTime;
   }
   public void setLastStatusTime(String lastStatusTime) {
      this.lastStatusTime = lastStatusTime;
   }
   public String getDeviceSubscription() {
      return deviceSubscription;
   }
   public void setDeviceSubscription(String deviceSubscription) {
      this.deviceSubscription = deviceSubscription;
   }
   public int[] getDeviceCapabilities() {
      return DeviceCapabilities;
   }
   public void setDeviceCapabilities(int[] deviceCapabilities) {
      DeviceCapabilities = deviceCapabilities;
   }
   public SlotConfiguration getSlotConfiguration() {
      return slotConfiguration;
   }
   public void setSlotConfiguration(SlotConfiguration slotConfiguration) {
      this.slotConfiguration = slotConfiguration;
   }
   public List<DeviceManagement> getDeviceManagement() {
      return deviceManagement;
   }
   public void setDeviceManagement(List<DeviceManagement> deviceManagement) {
      this.deviceManagement = deviceManagement;
   }
   public String getActions() {
      return actions;
   }
   public void setActions(String actions) {
      this.actions = actions;
   }
   public String getSensorHealthLink() {
      return sensorHealthLink;
   }
   public void setSensorHealthLink(String sensorHealthLink) {
      this.sensorHealthLink = sensorHealthLink;
   }
   public CommonObject getVirtualSession() {
      return virtualSession;
   }
   public void setVirtualSession(CommonObject virtualSession) {
      this.virtualSession = virtualSession;
   }
   public CommonObject getBaselines() {
      return baselines;
   }
   public void setBaselines(CommonObject baselines) {
      this.baselines = baselines;
   }
   public String getInventoryDetailsLink() {
      return inventoryDetailsLink;
   }
   public void setInventoryDetailsLink(String inventoryDetailsLink) {
      this.inventoryDetailsLink = inventoryDetailsLink;
   }
   public String getHardwareLogsLink() {
      return hardwareLogsLink;
   }
   public void setHardwareLogsLink(String hardwareLogsLink) {
      this.hardwareLogsLink = hardwareLogsLink;
   }
   public String getSubSystemHealthLink() {
      return subSystemHealthLink;
   }
   public void setSubSystemHealthLink(String subSystemHealthLink) {
      this.subSystemHealthLink = subSystemHealthLink;
   }
   public String getRecentActivityLink() {
      return recentActivityLink;
   }
   public void setRecentActivityLink(String recentActivityLink) {
      this.recentActivityLink = recentActivityLink;
   }
   public CommonObject getInventoryTypes() {
      return inventoryTypes;
   }
   public void setInventoryTypes(CommonObject inventoryTypes) {
      this.inventoryTypes = inventoryTypes;
   }
   public CommonObject getLogSeverities() {
      return logSeverities;
   }
   public void setLogSeverities(CommonObject logSeverities) {
      this.logSeverities = logSeverities;
   }
   public String getSettingLink() {
      return settingLink;
   }
   public void setSettingLink(String settingLink) {
      this.settingLink = settingLink;
   }
   public CommonObject getTemperature() {
      return temperature;
   }
   public void setTemperature(CommonObject temperature) {
      this.temperature = temperature;
   }
   public CommonObject getPower() {
      return power;
   }
   public void setPower(CommonObject power) {
      this.power = power;
   }
   public CommonObject getSystemUpTime() {
      return systemUpTime;
   }
   public void setSystemUpTime(CommonObject systemUpTime) {
      this.systemUpTime = systemUpTime;
   }
   public CommonObject getBlinkStatus() {
      return blinkStatus;
   }
   public void setBlinkStatus(CommonObject blinkStatus) {
      this.blinkStatus = blinkStatus;
   }
   public String getPowerUsageByDeviceLink() {
      return powerUsageByDeviceLink;
   }
   public void setPowerUsageByDeviceLink(String powerUsageByDeviceLink) {
      this.powerUsageByDeviceLink = powerUsageByDeviceLink;
   }
   public String getDeviceBladeSlotsLink() {
      return deviceBladeSlotsLink;
   }
   public void setDeviceBladeSlotsLink(String deviceBladeSlotsLink) {
      this.deviceBladeSlotsLink = deviceBladeSlotsLink;
   }
   public CommonObject getGraphicInfo() {
      return graphicInfo;
   }
   public void setGraphicInfo(CommonObject graphicInfo) {
      this.graphicInfo = graphicInfo;
   }
   public CommonObject getDeployRequired() {
      return deployRequired;
   }
   public void setDeployRequired(CommonObject deployRequired) {
      this.deployRequired = deployRequired;
   }

}
