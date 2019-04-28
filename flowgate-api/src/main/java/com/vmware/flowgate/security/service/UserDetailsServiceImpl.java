/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.security.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.vmware.flowgate.common.model.WormholeUser;
import com.vmware.flowgate.repository.UserRepository;
import com.vmware.flowgate.util.AuthorityUtil;
import com.vmware.flowgate.util.WormholeUserDetails;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

   @Autowired
   private UserRepository userRepository;

   @Override
   public WormholeUserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
      WormholeUser user = getUserByName(userName);
      AuthorityUtil authorityUtil = new AuthorityUtil();
      List<GrantedAuthority> privileges = new ArrayList<GrantedAuthority>();
      if (user == null) {
         throw new UsernameNotFoundException(
               String.format("No user found with username '%s'.", userName));
      }
      privileges = authorityUtil.createGrantedAuthorities(user.getRoleNames());
      return new WormholeUserDetails(user.getId(), user.getUserName(), user.getPassword(),
            privileges);
   }

   public WormholeUser getUserByName(String userName) {
      return userRepository.findOneByUserName(userName);
   }

}
