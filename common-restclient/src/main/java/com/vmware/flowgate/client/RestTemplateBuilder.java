/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.client;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

public class RestTemplateBuilder {


   public static RestTemplate buildDefaultRestTemplate()
         throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
      return buildTemplate(true, 60000);
   }

   public static RestTemplate buildTemplate(boolean verifyCert, int socketTimeout)
         throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
      SSLContext sslContext = SSLContext.getInstance("SSL");
      if (!verifyCert) {
         TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
         sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
      }
      SSLConnectionSocketFactory sslSocketFactory =
            new SSLConnectionSocketFactory(sslContext, new HostnameVerifier() {
               @Override
               public boolean verify(String urlHostName, SSLSession session) {
                  if (verifyCert) {
                     return HttpsURLConnection.getDefaultHostnameVerifier().verify(urlHostName,
                           session);
                  } else {
                     return true;
                  }
               }
            });
      Registry<ConnectionSocketFactory> registry =
            RegistryBuilder.<ConnectionSocketFactory> create()
                  .register("http", PlainConnectionSocketFactory.getSocketFactory())
                  .register("https", sslSocketFactory).build();
      PoolingHttpClientConnectionManager connectionManager =
            new PoolingHttpClientConnectionManager(registry);
      //extract this as a parameter if necessary.
      connectionManager.setMaxTotal(5);
      connectionManager.setDefaultMaxPerRoute(5);
      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
            .setConnectTimeout(socketTimeout).setConnectionRequestTimeout(socketTimeout).build();

      HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager).build();
      ClientHttpRequestFactory facotry = new HttpComponentsClientHttpRequestFactory(client);
      RestTemplate restTemplate = new RestTemplate(facotry);
      List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
      converters.add(new MappingJackson2HttpMessageConverter()); //json
      converters.add(new ResourceHttpMessageConverter()); //octet-stream
      converters.add(new StringHttpMessageConverter()); //String
      restTemplate.setMessageConverters(converters);
      return restTemplate;
   }

   public static HttpHeaders getDefaultHeader() {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      List<MediaType> acceptedTypes = new ArrayList<MediaType>();
      acceptedTypes.add(MediaType.APPLICATION_JSON);
      acceptedTypes.add(MediaType.TEXT_HTML);
      headers.setAccept(acceptedTypes);
      return headers;
   }

   public static HttpEntity<String> getDefaultEntity() {
      return new HttpEntity<String>(getDefaultHeader());
   }
}
