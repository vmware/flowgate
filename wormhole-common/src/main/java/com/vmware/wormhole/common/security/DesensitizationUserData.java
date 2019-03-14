/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.common.security;

import java.util.List;

import com.vmware.wormhole.common.model.WormholeUser;

public class DesensitizationUserData {

   public static WormholeUser desensitizationUser(WormholeUser user) {
      user.setPassword(null);
      return user;
   }
   
   public static List<WormholeUser> desensitizationUser(List<WormholeUser> users) {
      for(WormholeUser user:users) {
         desensitizationUser(user);
      }
      return users;
   }
}
