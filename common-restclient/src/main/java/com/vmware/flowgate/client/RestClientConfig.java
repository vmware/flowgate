/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

   @Value("${wormhole.apiclient.socket.timeout:20000}")
   private int socketTimeout;
   @Value("${wormhole.apiclient.connectionrequest.timeout:30000}")
   private int connectionRequestTimeout;

   @Bean(name = "httpRequestFactory")
   public ClientHttpRequestFactory httpRequestFactory() {
      return new HttpComponentsClientHttpRequestFactory(httpClient());
   }

   @Bean(name = "restTemplate")
   public RestTemplate restTemplate() {
      RestTemplate restTemplate = new RestTemplate(httpRequestFactory());
      List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
      converters.add(new MappingJackson2HttpMessageConverter()); //json
      converters.add(new ResourceHttpMessageConverter()); //octet-stream
      converters.add(new StringHttpMessageConverter()); //String
      restTemplate.setMessageConverters(converters);
      return restTemplate;
   }

   @Bean(name = "httpClient")
   public HttpClient httpClient() {
      Registry<ConnectionSocketFactory> registry =
            RegistryBuilder.<ConnectionSocketFactory> create()
                  .register("http", PlainConnectionSocketFactory.getSocketFactory())
                  .register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
      PoolingHttpClientConnectionManager connectionManager =
            new PoolingHttpClientConnectionManager(registry);
      // all the number should read from application.properties
      connectionManager.setMaxTotal(5);
      connectionManager.setDefaultMaxPerRoute(5);
      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
            .setConnectTimeout(socketTimeout).setConnectionRequestTimeout(connectionRequestTimeout).build();

      return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager).build();
   }
}
