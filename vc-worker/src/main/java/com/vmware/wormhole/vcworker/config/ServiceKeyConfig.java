/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.vcworker.config;

import org.springframework.context.annotation.Configuration;

import com.vmware.wormhole.common.WormholeConstant;

@Configuration
public class ServiceKeyConfig {

   private String serviceKey;
   
   public ServiceKeyConfig() {
      this.serviceKey = System.getenv(WormholeConstant.serviceKey);
   }
   
   public String getServiceKey() {
      return serviceKey;
   }
}
