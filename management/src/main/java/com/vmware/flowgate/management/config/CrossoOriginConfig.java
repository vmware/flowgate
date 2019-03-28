/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CrossoOriginConfig {

   @Value("${management.cross.allowedOrigin}")
   protected String[] allowedOrigin;
   @Value("${management.cross.allowedMethod}")
   protected String[] allowedMethods;
   @Value("${management.cross.allowedHeader}")
   protected String[] allowedHeaders;

   @Bean
   public CorsFilter corsFilter() {
      final UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource =
            new UrlBasedCorsConfigurationSource();
      final CorsConfiguration corsConfiguration = new CorsConfiguration();
      corsConfiguration.setAllowCredentials(true);
      for (String allowOrigin : allowedOrigin) {
         corsConfiguration.addAllowedOrigin(allowOrigin);
      }
      for (String allowHeader : allowedHeaders) {
         corsConfiguration.addAllowedHeader(allowHeader);
      }
      for (String allowMethod : allowedMethods) {
         corsConfiguration.addAllowedMethod(allowMethod);
      }
      urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
      return new CorsFilter(urlBasedCorsConfigurationSource);
   }

}
