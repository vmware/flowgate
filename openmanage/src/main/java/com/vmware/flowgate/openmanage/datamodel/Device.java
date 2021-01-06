/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Device {
   @JsonProperty(value="Id")
   private int id;
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

   //@JsonProperty(value="DeviceManagement")

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

}
