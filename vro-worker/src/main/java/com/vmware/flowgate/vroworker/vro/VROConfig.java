/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker.vro;

import com.vmware.flowgate.common.model.SDDCSoftwareConfig;

public class VROConfig {
   private String id;
   private String serverUrl;
   private String userName;
   private String password;
   private int port;
   private String locale;
   private String timeZone;
   private String verifyCert;
   private boolean useInternalAPI = true;
   private boolean ignoreHostName = true;

   public VROConfig() {

   }

   public VROConfig(SDDCSoftwareConfig config) {
      this.id = config.getId();
      this.serverUrl = config.getServerURL();
      this.userName = config.getUserName();
      this.password = config.getPassword();
      this.verifyCert = String.valueOf(config.isVerifyCert());
      this.locale = "en-us";
      this.timeZone = "PST";
   }

   public VROConfig(String serverUrl, String userName, String password, int port, String locale,
         String timeZone, String verifyCert, boolean useInternalAPI, boolean ignoreHostName) {
      super();
      this.serverUrl = serverUrl;
      this.userName = userName;
      this.password = password;
      this.port = port;
      this.locale = locale;
      this.timeZone = timeZone;
      this.verifyCert = verifyCert;
      this.useInternalAPI = useInternalAPI;
      this.ignoreHostName = ignoreHostName;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getServerUrl() {
      return serverUrl;
   }

   public void setServerUrl(String serverUrl) {
      this.serverUrl = serverUrl;
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

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public String getLocale() {
      return locale;
   }

   public void setLocale(String locale) {
      this.locale = locale;
   }

   public String getTimeZone() {
      return timeZone;
   }

   public void setTimeZone(String timeZone) {
      this.timeZone = timeZone;
   }

   public String getVerifyCert() {
      return verifyCert;
   }

   public void setVerifyCert(String verifyCert) {
      this.verifyCert = verifyCert;
   }

   public boolean isUseInternalAPI() {
      return useInternalAPI;
   }

   public void setUseInternalAPI(boolean useInternalAPI) {
      this.useInternalAPI = useInternalAPI;
   }

   public boolean isIgnoreHostName() {
      return ignoreHostName;
   }

   public void setIgnoreHostName(boolean ignoreHostName) {
      this.ignoreHostName = ignoreHostName;
   }

}
