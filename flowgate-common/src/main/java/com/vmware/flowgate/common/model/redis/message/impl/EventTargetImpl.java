/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model.redis.message.impl;

import java.util.Set;

import com.vmware.flowgate.common.model.redis.message.EventTarget;
import com.vmware.flowgate.common.model.redis.message.EventUser;

public class EventTargetImpl implements EventTarget {

   private static final long serialVersionUID = 1L;

   private Set<EventUser> users;

   public EventTargetImpl() {
   };

   public EventTargetImpl(Set<EventUser> users) {
      this.users = users;
   }

   public Set<EventUser> getUsers() {
      return users;
   }

   public void setUsers(Set<EventUser> users) {
      this.users = users;
   }

}
