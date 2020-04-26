/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.util;

import java.net.URI;

public class HandleURL {

   public static final String IP_REGX = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
         + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
         + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
         + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";

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
