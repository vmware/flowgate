/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.security.service;

import java.util.Collection;
import java.util.Iterator;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.vmware.flowgate.config.InitializeConfigureData;

@Service
public class AccessControlManager implements AccessDecisionManager {

   @Override
   public void decide(Authentication authentication, Object object,
         Collection<ConfigAttribute> configAttributes)
         throws AccessDeniedException, InsufficientAuthenticationException {

      ConfigAttribute configAttribute;
      String privilegeName;
      for (Iterator<ConfigAttribute> iterator = configAttributes.iterator(); iterator.hasNext();) {
         configAttribute = iterator.next();
         privilegeName = configAttribute.getAttribute();
         for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            if (privilegeName.trim().equals(grantedAuthority.getAuthority())) {
               return;
            }
         }
         if(InitializeConfigureData.Default_Access_Privilege.equals(privilegeName)) {
            return;
         }
      }
      throw new AccessDeniedException("no right");
   }

   public boolean supports(ConfigAttribute attribute) {
      return true;
   }

   public boolean supports(Class<?> clazz) {
      return true;
   }

}
