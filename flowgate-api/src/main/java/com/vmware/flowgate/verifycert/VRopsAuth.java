/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.verifycert;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.ops.api.client.Client;
import com.vmware.ops.api.client.Client.ClientConfig;
import com.vmware.ops.api.model.auth.AuthToken;
import com.vmware.ops.api.model.auth.UsernamePassword;

public class VRopsAuth {
   private Client client;

   public VRopsAuth(SDDCSoftwareConfig config) {
      String url = String.format("https://%s/suite-api", config.getServerURL());
      this.client = ClientConfig.builder()
            .useJson().serverUrl(url).verify(String.valueOf(config.isVerifyCert())).useClusterTruststore(true)
            .ignoreHostName(!config.isVerifyCert()).useInternalApis(false).build().newClient();
      UsernamePassword up = new UsernamePassword(config.getUserName(), config.getPassword());
      AuthToken token = client.userAndAuthManagementClient().acquireToken(up);
      client = ClientConfig.builder().tokenAuth(token.getToken()).useJson().serverUrl(url)
            .verify(String.valueOf(config.isVerifyCert())).ignoreHostName(!config.isVerifyCert())
            .useInternalApis(false).build().newClient();
   }

   public Client getClient() {
      return client;
   }


   protected String multilineString(Object obj) {
      return ReflectionToStringBuilder.reflectionToString(obj, ToStringStyle.MULTI_LINE_STYLE);
   }


}

