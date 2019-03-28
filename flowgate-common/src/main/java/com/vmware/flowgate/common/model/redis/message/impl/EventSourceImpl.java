/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model.redis.message.impl;

import com.vmware.flowgate.common.model.redis.message.EventSource;
import com.vmware.flowgate.common.model.redis.message.EventUser;

public class EventSourceImpl implements EventSource {

   private static final long serialVersionUID = 1L;

   private EventUser user;

   public EventSourceImpl() {};
   public EventUser getUser() {
      return user;
   }

   public void setUser(EventUser user) {
      this.user = user;
   }

}
