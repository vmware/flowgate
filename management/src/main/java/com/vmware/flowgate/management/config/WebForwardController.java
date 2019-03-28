/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebForwardController {

   @Bean
   public WebMvcConfigurerAdapter forwardToIndex() {
      return new WebMvcConfigurerAdapter() {
         @Override
         public void addViewControllers(ViewControllerRegistry registry) {
            registry.addViewController("/ui/**").setViewName("forward:/index.html");
         }
      };
   }
}
