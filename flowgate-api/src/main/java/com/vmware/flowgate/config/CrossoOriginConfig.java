/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CrossoOriginConfig {

   @Value("${api.cross.allowedOrigin}")
   protected String[] allowedOrigin;
   @Value("${api.cross.allowedMethod}")
   protected String[] allowedMethods;
   @Value("${api.cross.allowedHeader}")
   protected String[] allowedHeaders;
   @Bean
   public CorsFilter corsFilter() {
       final UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
       final CorsConfiguration corsConfiguration = new CorsConfiguration();
       corsConfiguration.setAllowCredentials(true);
       for(int i =0;i<allowedOrigin.length;i++) {
          corsConfiguration.addAllowedOriginPattern(allowedOrigin[i]);
       }
       for(int i =0;i<allowedHeaders.length;i++) {
          corsConfiguration.addAllowedHeader(allowedHeaders[i]);
       }
       for(int i =0;i<allowedMethods.length;i++) {
          corsConfiguration.addAllowedMethod(allowedMethods[i]);
       }
       urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
       return new CorsFilter(urlBasedCorsConfigurationSource);
   }
}
