/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model.redis.message.impl;

import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventSource;
import com.vmware.flowgate.common.model.redis.message.EventTarget;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.EventUser;

public class EventMessageImpl
      implements EventMessage {

   private static final long serialVersionUID = 1L;
   private EventType type;
   private EventUser eventUser;
   private EventSource source;
   private EventTarget target;
   private long createTime;
   private String content;

   public EventMessageImpl() {

   }

   public EventMessageImpl(EventType type, EventUser eventUser, EventSource source, EventTarget target, long createTime,
         String content) {
      super();
      this.type = type;
      this.eventUser = eventUser;
      this.source = source;
      this.target = target;
      this.createTime = createTime;
      this.content = content;
   }

   public EventType getType() {
      return type;
   }

   public EventSource getSource() {
      return source;
   }

   public EventTarget getTarget() {
      return target;
   }

   public String getContent() {
      return content;
   }

   public long getCreateTime() {
      return createTime;
   }

   public EventUser getEventUser() {
      return eventUser;
   }

   public void setEventUser(EventUser eventUser) {
      this.eventUser = eventUser;
   }

   public void setType(EventType type) {
      this.type = type;
   }

   public void setSource(EventSource source) {
      this.source = source;
   }

   public void setTarget(EventTarget target) {
      this.target = target;
   }

   public void setCreateTime(long createTime) {
      this.createTime = createTime;
   }

   public void setContent(String content) {
      this.content = content;
   }

}
