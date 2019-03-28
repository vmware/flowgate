/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.vmware.flowgate.config.InitializeConfigureData;

public class AuthorityUtil {

   public List<GrantedAuthority> createGrantedAuthorities(List<String> roleNames) {
      List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
      Set<String> privilegeNames = new HashSet<String>();
      for(String roleName:roleNames) {
         List<String> privileges = InitializeConfigureData.getPrivileges(roleName);
         if(privileges != null) {
            privilegeNames.addAll(privileges);
         }
      }
      for(String privilegeName:privilegeNames) {
         GrantedAuthority authority = new EnhanceSimpleGrantedAuthority(privilegeName);
         authorities.add(authority);
      }
      return authorities;
  }

   public static List<GrantedAuthority> createGrantedAuthorities(String authorities[]) {
      return Stream.of(authorities).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
  }

   public String[] getAuthorities(UserDetails user) {
      return user.getAuthorities().stream().map(GrantedAuthority::<String>getAuthority).toArray(String[]::new);
   }

   public Set<String> getPrivilege(UserDetails user) {
      Set<String> privilege = new HashSet<String>();
      for(GrantedAuthority grantedAuthority:user.getAuthorities()) {
         privilege.add(grantedAuthority.getAuthority());
      }
      return privilege;
   }

}
