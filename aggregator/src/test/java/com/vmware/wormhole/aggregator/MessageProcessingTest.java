/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.aggregator;

import java.io.IOException;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.wormhole.common.model.SDDCSoftwareConfig;
import com.vmware.wormhole.common.model.SDDCSoftwareConfig.SoftwareType;
import com.vmware.wormhole.common.model.redis.message.EventMessage;
import com.vmware.wormhole.common.model.redis.message.EventType;
import com.vmware.wormhole.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.wormhole.common.model.redis.message.impl.EventMessageUtil;

public class MessageProcessingTest {

   @Autowired
   private StringRedisTemplate template;

   @Test
   public void testMessage() {
      ObjectMapper mapper = new ObjectMapper();
      String template = "{\"type\":\"InfoBlox\",\"eventUser\":null,\"source\":null,\"target\":null,\"createTime\":%s,\"content\":\"nihao\"}";
      long time = new Date().getTime();
      EventMessage message =
            new EventMessageImpl(
                  EventType.InfoBlox, null, null, null, time, "nihao");
      String bb;
      try {
      bb = mapper.writeValueAsString(message);
      Assert.assertEquals(String.format(template, time), bb);

      EventMessage mess2 = mapper.readValue(bb, EventMessageImpl.class);
      Assert.assertEquals("nihao",mess2.getContent());

      String gg = "{\"type\":\"InfoBlox\",\"createTime\":1539073715966,\"content\":\"helloworld\"}";
      EventMessage mess3 = mapper.readValue(gg, EventMessageImpl.class);
      Assert.assertEquals("helloworld", mess3.getContent());
      }catch(IOException e) {
        Assert.fail();
      }
   }

   @Test
   public void testVC() throws Exception{
      ObjectMapper mapper = new ObjectMapper();
      SDDCSoftwareConfig vc = new SDDCSoftwareConfig();
      vc.setDescription("good vc");
      vc.setName("Test VC");
      vc.setServerURL("10.10.10.10");
      vc.setPassword("fake password");
      vc.setType(SoftwareType.VCENTER);
      String payload = mapper.writeValueAsString(vc);
//      EventMessage message =
//            EventMessageUtil.createEventMessage(EventType.VCenter, EventMessageUtil.VCENTER_SyncCustomerAttrs, payload);
      EventMessage message = EventMessageUtil.createEventMessage(EventType.VCenter, EventMessageUtil.VCENTER_SyncData, "");
      System.out.println(mapper.writeValueAsString(message));
   }
}
