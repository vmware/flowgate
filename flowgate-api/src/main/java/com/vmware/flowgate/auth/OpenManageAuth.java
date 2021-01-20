/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.auth;

import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vmware.flowgate.common.model.FacilitySoftwareConfig;

public class OpenManageAuth extends ServerAuth {

   private String authUrl = "%s/api/SessionService/Sessions";
   private String apiSession = "API";
   private String userName = "UserName";
   private String password = "Password";
   private String sessionType = "SessionType";
   private String authHeader = "x-auth-token";
   private String logoutUrl = "%s/api/SessionService/Actions/SessionService.Logoff";

   public boolean auth(FacilitySoftwareConfig intergration)
         throws KeyManagementException, SSLException, UnknownHostException, CertificateException,
         NoSuchAlgorithmException, KeyStoreException {
      RestTemplate restTemplate = getConnection(intergration);
      Map<String,String> authInfo = new HashMap<String,String>();
      authInfo.put(userName, intergration.getUserName());
      authInfo.put(password, intergration.getPassword());
      authInfo.put(sessionType, apiSession);
      HttpEntity<Object> postEntity = new HttpEntity<Object>(authInfo, getDefaultHeader());
      ResponseEntity<Void> entity =
            restTemplate.exchange(String.format(authUrl, intergration.getServerURL()),
                  HttpMethod.POST, postEntity, Void.class);
      if (entity.getStatusCode().is2xxSuccessful()
            && !entity.getHeaders().get(authHeader).isEmpty()) {
         String token = entity.getHeaders().get(authHeader).get(0);
         HttpHeaders headers = getDefaultHeader();
         headers.add(authHeader, token);
         restTemplate.exchange(String.format(logoutUrl, intergration.getServerURL()),
               HttpMethod.POST, new HttpEntity<String>(headers), Void.class);
         return true;
      }
      return false;
   }

   private HttpHeaders getDefaultHeader() {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      List<MediaType> acceptedTypes = new ArrayList<MediaType>();
      acceptedTypes.add(MediaType.APPLICATION_JSON);
      acceptedTypes.add(MediaType.TEXT_HTML);
      headers.setAccept(acceptedTypes);
      return headers;
   }

}
