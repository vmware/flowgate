/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.auth;

import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.RestTemplate;

import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.util.HandleURL;

public class PowerIQAuth extends ServerAuth {
   private static final String authUrl = "%s/api/v2/aisles";

   public boolean auth(FacilitySoftwareConfig softwareConfig)
         throws KeyManagementException, SSLException, UnknownHostException,
         CertificateException, NoSuchAlgorithmException, KeyStoreException {
      RestTemplate restTemplate = getConnection(softwareConfig);
      restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(
            softwareConfig.getUserName(), softwareConfig.getPassword()));
      ResponseEntity<String> resp = restTemplate
            .getForEntity(String.format(authUrl,softwareConfig.getServerURL()), String.class);

      if (HttpStatus.MOVED_PERMANENTLY.equals(resp.getStatusCode())) {
         HandleURL handleURL = new HandleURL();
         String url = resp.getHeaders().get("Location").get(0);
         ResponseEntity<String> response =
               restTemplate.getForEntity(String.format(url, authUrl), String.class);
         if (HttpStatus.OK.equals(response.getStatusCode())) {
            softwareConfig.setServerURL(handleURL.formatURL(url));
            return true;
         }
         return false;
      } else if (HttpStatus.OK.equals(resp.getStatusCode())) {
         return true;
      }
      return false;
   }
}
