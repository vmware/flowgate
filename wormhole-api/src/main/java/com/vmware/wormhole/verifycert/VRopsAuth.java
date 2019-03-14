/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.verifycert;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import com.vmware.ops.api.client.Client;
import com.vmware.ops.api.client.Client.ClientConfig;
import com.vmware.wormhole.common.model.SDDCSoftwareConfig;

public class VRopsAuth {
   private  Client client;
   public VRopsAuth(SDDCSoftwareConfig config) {
      String url = String.format("https://%s/suite-api", config.getServerURL());
      try {
         this.client = ClientConfig.builder().basicAuth(config.getUserName(), config.getPassword())
               .useJson()
               .serverUrl(url).verify(String.valueOf(config.isVerifyCert()))
               .ignoreHostName(!config.isVerifyCert()).useInternalApis(false)
               .build().newClient();
      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   public Client getClient() {
      return client;
   }


   protected String multilineString(Object obj) {
      return ReflectionToStringBuilder.reflectionToString(obj, ToStringStyle.MULTI_LINE_STYLE);
   }


}

