/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.labsdb.redis;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.redis.message.AsyncService;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.MessageReceiver;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;

@Component
public class RedisMessageReceiver implements MessageReceiver {

   @Autowired
   private AsyncService asyncService;
   private static final Logger logger = LoggerFactory.getLogger(RedisMessageReceiver.class);
   private ObjectMapper mapper = new ObjectMapper();
   @Override
   public void receiveMessage(String message) {
      try {
      EventMessage eventMessage = mapper.readValue(message, EventMessageImpl.class);
      asyncService.executeAsync(eventMessage);
      }catch(IOException e) {
         logger.info(String.format("Failed prase message %s",message));
      }
   }
   @Override
   public void receiveNotificationMessage(String message) {
      // TODO Auto-generated method stub
      System.out.println(message);
   }

}
