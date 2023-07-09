/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@EnableZuulProxy
@SpringBootApplication(scanBasePackages = "com.vmware.flowgate",exclude = {
            ErrorMvcAutoConfiguration.class})
public class ManagementPortalApplication {

   public static void main(String[] args) {
      SpringApplication.run(ManagementPortalApplication.class, args);
      System.out.println(">>>>>>>>>>>>>>>>Welcome to use Management console!<<<<<<<<<<<<<<<<");
   }
}