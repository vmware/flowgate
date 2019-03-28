/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.security.filter;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.stereotype.Service;

import com.vmware.flowgate.security.service.AccessControlManager;
import com.vmware.flowgate.security.service.PrepareSecurityMetadataSourceService;

@Service
public class AccessSecurityMetadataInterceptor extends FilterSecurityInterceptor {

   @Autowired
   private PrepareSecurityMetadataSourceService  securityMetadataSource;

   @Autowired
   private AuthenticationManager authenticationManager;

   @Autowired
   AccessControlManager accessControlManager;

   @PostConstruct
   public void init() {
       super.setAccessDecisionManager(accessControlManager);
       super.setAuthenticationManager(authenticationManager);
   }

   @Override
   public SecurityMetadataSource obtainSecurityMetadataSource() {
      return this.securityMetadataSource;
   }


}
