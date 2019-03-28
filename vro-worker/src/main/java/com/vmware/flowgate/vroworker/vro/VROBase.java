/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker.vro;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.vmware.ops.api.client.Client;
import com.vmware.ops.api.client.Client.ClientConfig;

public abstract class VROBase {
   private final Client client;

   public VROBase(VROConfig config) {

      //
      if (config == null) {
         config = getDefautlConfig();
      }
      try {
         this.client = ClientConfig.builder().basicAuth(config.getUserName(), config.getPassword())
               .useJson().locale(config.getLocale()).timezone(config.getTimeZone())
               .serverUrl(String.format(VROConsts.VROSDKURL, config.getServerUrl()))
               .verify(config.getVerifyCert()).ignoreHostName(config.isIgnoreHostName())
               .useInternalApis(config.isUseInternalAPI()).build().newClient();
      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   protected Client getClient() {
      return client;
   }

   private VROConfig getDefautlConfig() {
      VROConfig config = new VROConfig();
      config.setIgnoreHostName(true);
      config.setUserName("admin");
      config.setPassword("Admin!23");
      config.setServerUrl("https://10.112.113.180/suite-api");
      config.setPort(443);
      config.setVerifyCert("false");
      config.setUseInternalAPI(false);
      return config;
   }

   protected String multilineString(Object obj) {
      return ReflectionToStringBuilder.reflectionToString(obj, ToStringStyle.MULTI_LINE_STYLE);
   }

   public abstract void run();
}
