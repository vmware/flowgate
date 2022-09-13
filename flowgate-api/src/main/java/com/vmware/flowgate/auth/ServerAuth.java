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
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.vmware.flowgate.common.model.FacilitySoftwareConfig;

public class ServerAuth {

   public RestTemplate getConnection(FacilitySoftwareConfig softwareConfig)
         throws SSLException, UnknownHostException, CertificateException, NoSuchAlgorithmException,
         KeyStoreException, KeyManagementException {
      SSLContext sslContext = null;
      RestTemplate restTemplate;
      if (!softwareConfig.isVerifyCert()) {
         sslContext =
               org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(new TrustStrategy() {
                  @Override
                  public boolean isTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                     return true;
                  }
               }).build();
         SSLConnectionSocketFactory csf =
               new SSLConnectionSocketFactory(sslContext, new HostnameVerifier() {
                  @Override
                  public boolean verify(String urlHostName, SSLSession session) {
                     if (softwareConfig.isVerifyCert()) {
                        return HttpsURLConnection.getDefaultHostnameVerifier().verify(urlHostName,
                              session);
                     } else {
                        return true;
                     }
                  }
               });
         CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
         HttpComponentsClientHttpRequestFactory requestFactory =
               new HttpComponentsClientHttpRequestFactory();
         requestFactory.setHttpClient(httpClient);
         restTemplate = new RestTemplate(requestFactory);
      } else {
         restTemplate = new RestTemplate();
      }
      return restTemplate;
   }
}
