/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.util;

import java.net.URI;

public class HandleURL {

   public String formatURL(String url) {
      URI serverUri = URI.create(url);
      StringBuffer serverUrl = new StringBuffer();
      if(serverUri.getScheme()!=null) {
         serverUrl.append(serverUri.getScheme()+"://");
      }else {
         serverUrl.append("https://");
         serverUri = URI.create(serverUrl.toString()+url);
      }
      if(serverUri.getHost() != null) {
         serverUrl.append(serverUri.getHost());
      }
      if(serverUri.getPort() != -1) {
         serverUrl.append(":"+serverUri.getPort());
      }
      return serverUrl.toString();
   }
}
