/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model.redis.message.impl;

import com.vmware.flowgate.common.model.redis.message.EventUser;

public class EventUserImpl implements EventUser {

   private static final long serialVersionUID = 1L;
   private String id;

   public EventUserImpl() {};
   public EventUserImpl(String id) {
      this.id = id;
   }

   public String getId() {
      return id;
   }

}
