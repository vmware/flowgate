/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.util;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class WormholeUserDetails implements UserDetails {
   private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

   private String userId;
   private String password;
   private String username;
   private boolean accountNonExpired = true;
   private boolean accountNonLocked = true;
   private boolean credentialsNonExpired = true;
   private boolean enabled = true;
   @JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS, property = "class")
   private Collection<GrantedAuthority> authorities;

   public WormholeUserDetails() {

   }
   public WormholeUserDetails(String userId,String username, String password, Collection<GrantedAuthority> authroities) {
      this.userId = userId;
      this.username = username;
      this.password = password;
      this.authorities = authroities;
   }
   
   public String getUserId() {
      return userId;
   }
   
   public void setUserId(String userId) {
      this.userId = userId;
   }
   
   public void setPassword(String password) {
      this.password = password;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public void setAccountNonExpired(boolean accountNonExpired) {
      this.accountNonExpired = accountNonExpired;
   }

   public void setAccountNonLocked(boolean accountNonLocked) {
      this.accountNonLocked = accountNonLocked;
   }

   public void setCredentialsNonExpired(boolean credentialsNonExpired) {
      this.credentialsNonExpired = credentialsNonExpired;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public void setAuthorities(Collection<GrantedAuthority> authorities) {
      this.authorities = authorities;
   }

   @Override
   public Collection<? extends GrantedAuthority> getAuthorities() {
      return authorities;
   }

   @Override
   public String getPassword() {
      return this.password;
   }

   @Override
   public String getUsername() {
      return this.username;
   }

   @Override
   public boolean isAccountNonExpired() {
      return accountNonExpired;
   }

   @Override
   public boolean isAccountNonLocked() {
      return accountNonLocked;
   }

   @Override
   public boolean isCredentialsNonExpired() {
      return credentialsNonExpired;
   }

   @Override
   public boolean isEnabled() {
      return enabled;
   }

}
