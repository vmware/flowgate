/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.flowgate.common.model.redis.message.MessagePublisher;

@RestController
@RequestMapping("/v1/pub")
public class PublishMessageController {

   @Autowired
   private MessagePublisher publisher;

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/{topic}", method = RequestMethod.POST)
   public void pubMessage(@PathVariable String topic, @RequestBody String message) {
      publisher.publish(topic, message);
   }
}
