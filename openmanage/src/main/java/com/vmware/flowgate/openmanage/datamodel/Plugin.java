/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Plugin {

   @JsonProperty(value="Id")
   private String id;
   @JsonProperty(value="Name")
   private String name;
   @JsonProperty(value="UpdateAvailable")
   private boolean updateAvailable;
   @JsonProperty(value="Installed")
   private boolean installed;
   @JsonProperty(value="Enabled")
   private boolean enabled;
   @JsonProperty(value="Publisher")
   private String publisher;
   @JsonProperty(value="CurrentVersion")
   private String currentVersion;
   @JsonProperty(value="Description")
   private String description;
   @JsonProperty(value="InstalledDate")
   private String installedDate;
   @JsonProperty(value="LastUpdatedDate")
   private String lastUpdatedDate;
   @JsonProperty(value="LastDisabledDate")
   private String lastDisabledDate;
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public boolean isUpdateAvailable() {
      return updateAvailable;
   }
   public void setUpdateAvailable(boolean updateAvailable) {
      this.updateAvailable = updateAvailable;
   }
   public boolean isInstalled() {
      return installed;
   }
   public void setInstalled(boolean installed) {
      this.installed = installed;
   }
   public boolean isEnabled() {
      return enabled;
   }
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
   public String getPublisher() {
      return publisher;
   }
   public void setPublisher(String publisher) {
      this.publisher = publisher;
   }
   public String getCurrentVersion() {
      return currentVersion;
   }
   public void setCurrentVersion(String currentVersion) {
      this.currentVersion = currentVersion;
   }
   public String getDescription() {
      return description;
   }
   public void setDescription(String description) {
      this.description = description;
   }
   public String getInstalledDate() {
      return installedDate;
   }
   public void setInstalledDate(String installedDate) {
      this.installedDate = installedDate;
   }
   public String getLastUpdatedDate() {
      return lastUpdatedDate;
   }
   public void setLastUpdatedDate(String lastUpdatedDate) {
      this.lastUpdatedDate = lastUpdatedDate;
   }
   public String getLastDisabledDate() {
      return lastDisabledDate;
   }
   public void setLastDisabledDate(String lastDisabledDate) {
      this.lastDisabledDate = lastDisabledDate;
   }
}
