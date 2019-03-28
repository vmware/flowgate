/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EnhanceSimpleGrantedAuthority implements GrantedAuthority {

   private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;
   private String authority;

   @Override
   public String getAuthority() {
      return authority;
   }

   public EnhanceSimpleGrantedAuthority(@JsonProperty("authority") String privilege) {
      this.authority = privilege;
   }

}
