/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker.config;

import org.springframework.context.annotation.Configuration;

import com.vmware.flowgate.common.FlowgateConstant;

@Configuration
public class ServiceKeyConfig {

   private String serviceKey;
   
   public ServiceKeyConfig() {
      this.serviceKey = System.getenv(FlowgateConstant.serviceKey);
   }
   
   public String getServiceKey() {
      return serviceKey;
   }
}
