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

public class NlyteAuth extends ServerAuth {

   private static final String authUrl = "%s/nlyte/integration/api/odata/auth/AuthenticateBasic";

   public boolean auth(FacilitySoftwareConfig softwareConfig)
         throws  KeyManagementException, SSLException, UnknownHostException,
         CertificateException, NoSuchAlgorithmException, KeyStoreException {
      RestTemplate restTemplate = getConnection(softwareConfig);
      restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(
            softwareConfig.getUserName(), softwareConfig.getPassword()));
      ResponseEntity<String> response = restTemplate
            .getForEntity(String.format(authUrl,softwareConfig.getServerURL()), String.class);
      if (HttpStatus.OK.equals(response.getStatusCode())) {
         return true;
      }
      return false;
   }
}
