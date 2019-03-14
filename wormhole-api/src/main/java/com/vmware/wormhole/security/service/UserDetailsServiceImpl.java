/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.security.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.vmware.wormhole.common.model.WormholeUser;
import com.vmware.wormhole.repository.UserRepository;
import com.vmware.wormhole.util.AuthorityUtil;
import com.vmware.wormhole.util.WormholeUserDetails;

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
         throw new UsernameNotFoundException(String.format("No user found with username '%s'.", userName));
      }
      privileges = authorityUtil.createGrantedAuthorities(user.getRoleNames());
      return new WormholeUserDetails(user.getId(),user.getUserName(),user.getPassword(),privileges);
   }

   public WormholeUser getUserByName(String userName) {
      WormholeUser user = new WormholeUser();
      user.setUserName(userName);
      ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("lastPasswordResetDate");
      Example<WormholeUser> example = Example.of(user, matcher);
      return userRepository.findOne(example);
   }
   
}
