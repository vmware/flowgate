/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Pdu {

   private long id;
   @JsonProperty(value = "snmp3_enabled")
   private boolean snmp3Enabled;
   @JsonProperty(value = "snmp3_user")
   private String snmp3User;
   @JsonProperty(value = "snmp3_auth_level")
   private String snmp3AuthLevel;
   private String description;
   private String contact;
   @JsonProperty(value = "proxy_index")
   private long proxyIndex;
   @JsonProperty(value = "requires_manual_voltage")
   private String requiresManualVoltage;
   @JsonProperty(value = "configured_inlet_voltage")
   private String configuredInletVoltage;
   @JsonProperty(value = "configured_outlet_voltage")
   private String configuredOutletVoltage;
   @JsonProperty(value = "supports_single_sign_on")
   private boolean supportsSingleSignOn;
   @JsonProperty(value = "supports_firmware_upgrades")
   private boolean supportsFirmwareUpgrades;
   @JsonProperty(value = "supports_bulk_configuration")
   private boolean supportsBulkConfiguration;
   @JsonProperty(value = "supports_outlet_power_control")
   private boolean supportsOutletPowerControl;
   @JsonProperty(value = "supports_outlet_renaming")
   private boolean supportsOutletRenaming;
   private String model;
   private String location;
   @JsonProperty(value = "serial_number")
   private String serialNumber;
   private String manufacturer;
   @JsonProperty(value = "firmware_version")
   private String firmwareVersion;
   @JsonProperty(value = "poller_plugin")
   private String pollerPlugin;
   @JsonProperty(value = "rated_volts")
   private String ratedVolts;
   @JsonProperty(value = "rated_amps")
   private String ratedAmps;
   @JsonProperty(value = "rated_va")
   private String ratedVa;
   @JsonProperty(value = "ip_address")
   private String ipAddress;
   @JsonProperty(value = "inline_meter")
   private String inlineMeter;
   @JsonProperty(value = "supports_readingsonly_poll")
   private boolean supportsReadingsonlyPoll;
   @JsonProperty(value = "supports_data_logging")
   private boolean supportsDataLogging;
   @JsonProperty(value = "supports_sensor_renaming")
   private boolean supportsSensorRenaming;
   @JsonProperty(value = "default_connected_led_color")
   private String defaultConnectedLedColor;
   @JsonProperty(value = "default_disconnected_led_color")
   private String defaultDisconnectedLedColor;
   @JsonProperty(value = "dynamic_plugin_name")
   private String dynamicPluginName;
   private String phase;
   @JsonProperty(value = "user_defined_phase")
   private boolean userDefinedPhase;
   @JsonProperty(value = "custom_field_1")
   private String customField1;
   @JsonProperty(value = "custom_field_2")
   private String customField2;
   @JsonProperty(value = "external_key")
   private String externalKey;
   @JsonProperty(value = "decommissioned_at")
   private String decommissionedAt;
   @JsonProperty(value = "maintenance_enabled")
   private boolean maintenanceEnabled;
   private Parent parent;
   private String type;
   private String caption;
   @JsonProperty(value = "preferred_name")
   private String preferredName;
   private boolean decommissioned;
   private String name;
   private Reading reading;
   private Health health;

   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public boolean isSnmp3Enabled() {
      return snmp3Enabled;
   }

   public void setSnmp3Enabled(boolean snmp3Enabled) {
      this.snmp3Enabled = snmp3Enabled;
   }

   public String getSnmp3User() {
      return snmp3User;
   }

   public void setSnmp3User(String snmp3User) {
      this.snmp3User = snmp3User;
   }

   public String getSnmp3AuthLevel() {
      return snmp3AuthLevel;
   }

   public void setSnmp3AuthLevel(String snmp3AuthLevel) {
      this.snmp3AuthLevel = snmp3AuthLevel;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getContact() {
      return contact;
   }

   public void setContact(String contact) {
      this.contact = contact;
   }

   public long getProxyIndex() {
      return proxyIndex;
   }

   public void setProxyIndex(long proxyIndex) {
      this.proxyIndex = proxyIndex;
   }

   public String getRequiresManualVoltage() {
      return requiresManualVoltage;
   }

   public void setRequiresManualVoltage(String requiresManualVoltage) {
      this.requiresManualVoltage = requiresManualVoltage;
   }

   public String getConfiguredInletVoltage() {
      return configuredInletVoltage;
   }

   public void setConfiguredInletVoltage(String configuredInletVoltage) {
      this.configuredInletVoltage = configuredInletVoltage;
   }

   public String getConfiguredOutletVoltage() {
      return configuredOutletVoltage;
   }

   public void setConfiguredOutletVoltage(String configuredOutletVoltage) {
      this.configuredOutletVoltage = configuredOutletVoltage;
   }

   public boolean isSupportsSingleSignOn() {
      return supportsSingleSignOn;
   }

   public void setSupportsSingleSignOn(boolean supportsSingleSignOn) {
      this.supportsSingleSignOn = supportsSingleSignOn;
   }

   public boolean isSupportsFirmwareUpgrades() {
      return supportsFirmwareUpgrades;
   }

   public void setSupportsFirmwareUpgrades(boolean supportsFirmwareUpgrades) {
      this.supportsFirmwareUpgrades = supportsFirmwareUpgrades;
   }

   public boolean isSupportsBulkConfiguration() {
      return supportsBulkConfiguration;
   }

   public void setSupportsBulkConfiguration(boolean supportsBulkConfiguration) {
      this.supportsBulkConfiguration = supportsBulkConfiguration;
   }

   public boolean isSupportsOutletPowerControl() {
      return supportsOutletPowerControl;
   }

   public void setSupportsOutletPowerControl(boolean supportsOutletPowerControl) {
      this.supportsOutletPowerControl = supportsOutletPowerControl;
   }

   public boolean isSupportsOutletRenaming() {
      return supportsOutletRenaming;
   }

   public void setSupportsOutletRenaming(boolean supportsOutletRenaming) {
      this.supportsOutletRenaming = supportsOutletRenaming;
   }

   public String getModel() {
      return model;
   }

   public void setModel(String model) {
      this.model = model;
   }

   public String getLocation() {
      return location;
   }

   public void setLocation(String location) {
      this.location = location;
   }

   public String getSerialNumber() {
      return serialNumber;
   }

   public void setSerialNumber(String serialNumber) {
      this.serialNumber = serialNumber;
   }

   public String getManufacturer() {
      return manufacturer;
   }

   public void setManufacturer(String manufacturer) {
      this.manufacturer = manufacturer;
   }

   public String getFirmwareVersion() {
      return firmwareVersion;
   }

   public void setFirmwareVersion(String firmwareVersion) {
      this.firmwareVersion = firmwareVersion;
   }

   public String getPollerPlugin() {
      return pollerPlugin;
   }

   public void setPollerPlugin(String pollerPlugin) {
      this.pollerPlugin = pollerPlugin;
   }

   public String getRatedVolts() {
      return ratedVolts;
   }

   public void setRatedVolts(String ratedVolts) {
      this.ratedVolts = ratedVolts;
   }

   public String getRatedAmps() {
      return ratedAmps;
   }

   public void setRatedAmps(String ratedAmps) {
      this.ratedAmps = ratedAmps;
   }

   public String getRatedVa() {
      return ratedVa;
   }

   public void setRatedVa(String ratedVa) {
      this.ratedVa = ratedVa;
   }

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public String getInlineMeter() {
      return inlineMeter;
   }

   public void setInlineMeter(String inlineMeter) {
      this.inlineMeter = inlineMeter;
   }

   public boolean isSupportsReadingsonlyPoll() {
      return supportsReadingsonlyPoll;
   }

   public void setSupportsReadingsonlyPoll(boolean supportsReadingsonlyPoll) {
      this.supportsReadingsonlyPoll = supportsReadingsonlyPoll;
   }

   public boolean isSupportsDataLogging() {
      return supportsDataLogging;
   }

   public void setSupportsDataLogging(boolean supportsDataLogging) {
      this.supportsDataLogging = supportsDataLogging;
   }

   public boolean isSupportsSensorRenaming() {
      return supportsSensorRenaming;
   }

   public void setSupportsSensorRenaming(boolean supportsSensorRenaming) {
      this.supportsSensorRenaming = supportsSensorRenaming;
   }

   public String getDefaultConnectedLedColor() {
      return defaultConnectedLedColor;
   }

   public void setDefaultConnectedLedColor(String defaultConnectedLedColor) {
      this.defaultConnectedLedColor = defaultConnectedLedColor;
   }

   public String getDefaultDisconnectedLedColor() {
      return defaultDisconnectedLedColor;
   }

   public void setDefaultDisconnectedLedColor(String defaultDisconnectedLedColor) {
      this.defaultDisconnectedLedColor = defaultDisconnectedLedColor;
   }

   public String getDynamicPluginName() {
      return dynamicPluginName;
   }

   public void setDynamicPluginName(String dynamicPluginName) {
      this.dynamicPluginName = dynamicPluginName;
   }

   public String getPhase() {
      return phase;
   }

   public void setPhase(String phase) {
      this.phase = phase;
   }

   public boolean isUserDefinedPhase() {
      return userDefinedPhase;
   }

   public void setUserDefinedPhase(boolean userDefinedPhase) {
      this.userDefinedPhase = userDefinedPhase;
   }

   public String getCustomField1() {
      return customField1;
   }

   public void setCustomField1(String customField1) {
      this.customField1 = customField1;
   }

   public String getCustomField2() {
      return customField2;
   }

   public void setCustomField2(String customField2) {
      this.customField2 = customField2;
   }

   public String getExternalKey() {
      return externalKey;
   }

   public void setExternalKey(String externalKey) {
      this.externalKey = externalKey;
   }

   public String getDecommissionedAt() {
      return decommissionedAt;
   }

   public void setDecommissionedAt(String decommissionedAt) {
      this.decommissionedAt = decommissionedAt;
   }

   public boolean isMaintenanceEnabled() {
      return maintenanceEnabled;
   }

   public void setMaintenanceEnabled(boolean maintenanceEnabled) {
      this.maintenanceEnabled = maintenanceEnabled;
   }

   public Parent getParent() {
      return parent;
   }

   public void setParent(Parent parent) {
      this.parent = parent;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getCaption() {
      return caption;
   }

   public void setCaption(String caption) {
      this.caption = caption;
   }

   public String getPreferredName() {
      return preferredName;
   }

   public void setPreferredName(String preferredName) {
      this.preferredName = preferredName;
   }

   public boolean isDecommissioned() {
      return decommissioned;
   }

   public void setDecommissioned(boolean decommissioned) {
      this.decommissioned = decommissioned;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Reading getReading() {
      return reading;
   }

   public void setReading(Reading reading) {
      this.reading = reading;
   }

   public Health getHealth() {
      return health;
   }

   public void setHealth(Health health) {
      this.health = health;
   }

}
