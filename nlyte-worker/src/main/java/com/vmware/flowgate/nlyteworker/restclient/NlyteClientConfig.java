/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.restclient;

import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class NlyteClientConfig {
   

   public RestTemplate getRestTemplate(boolean isVerifyCert) throws Exception {
      
      SSLContext sslContext = SSLContext.getInstance("SSL");
      if(!isVerifyCert) {
         TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
         sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
      }
         SSLConnectionSocketFactory csf =
               new SSLConnectionSocketFactory(sslContext,new HostnameVerifier() {
                  @Override
                  public boolean verify(String urlHostName, SSLSession session) {
                     if(isVerifyCert) {
                        return HttpsURLConnection.getDefaultHostnameVerifier().verify(urlHostName, session);
                     }else {
                        return true;
                     }
                  }
              });
         CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
         HttpComponentsClientHttpRequestFactory requestFactory =
               new HttpComponentsClientHttpRequestFactory();
         requestFactory.setHttpClient(httpClient);
        return new RestTemplate(requestFactory);
   }
}
