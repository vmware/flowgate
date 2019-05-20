/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.vmware.flowgate.common.model.JobConfig;
import com.vmware.flowgate.common.model.JobConfig.JobType;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.common.utils.IPAddressUtil;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.JobsRepository;

@RestController
@RequestMapping("/v1/jobs")
public class JobsController {

   private static final Logger log = LoggerFactory.getLogger(JobsController.class);
   @Autowired
   private MessagePublisher publisher;

   @Autowired
   private JobsRepository jobsRepository;

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/mergeservermapping", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void startFullMappingAggregation() {
      try {
         EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.Aggregator,
               EventMessageUtil.FullMappingCommand, "");
         String message = EventMessageUtil.convertEventMessageAsString(eventMessage);
         publisher.publish(EventMessageUtil.AggregatorTopic, message);
      } catch (IOException e) {
         log.error("Failed to create event message", e);
         throw new WormholeRequestException("Failed to create event message");
      }
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/pduservermapping", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void generateServerPDUMapping() {
      try {
         EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.Aggregator,
               EventMessageUtil.PDUServerMappingCommand, "");
         String message = EventMessageUtil.convertEventMessageAsString(eventMessage);
         publisher.publish(EventMessageUtil.AggregatorTopic, message);
      } catch (IOException e) {
         log.error("Failed to create event message", e);
         throw new WormholeRequestException("Failed to create event message");
      }
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/temphumiditymapping/fullsync/{fullsync}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void generateTempHumidityMapping(@PathVariable boolean fullsync) {
      try {
         String command = fullsync ? EventMessageUtil.FullSyncTemperatureAndHumiditySensors
               : EventMessageUtil.SyncTemperatureAndHumiditySensors;
         EventMessage eventMessage =
               EventMessageUtil.createEventMessage(EventType.Aggregator, command, "");
         String message = EventMessageUtil.convertEventMessageAsString(eventMessage);
         publisher.publish(EventMessageUtil.AggregatorTopic, message);
      } catch (IOException e) {
         log.error("Failed to create event message", e);
         throw new WormholeRequestException("Failed to create event message");
      }
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/synchostnamebyip/{ip:.+}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void syncHostnameByIp(@PathVariable("ip") String ip) {
      if (IPAddressUtil.isValidIp(ip)) {
         EventMessage eventMessage =
               EventMessageUtil.createEventMessage(EventType.InfoBlox, null, ip);
         try {
            String message = EventMessageUtil.convertEventMessageAsString(eventMessage);
            publisher.publish(EventMessageUtil.InfobloxTopic, message);
         } catch (IOException e) {
            log.error("Failed to create event message", e);
            throw new WormholeRequestException(
                  "Failed to create event message for query hostname.");
         }
      } else {
         throw new WormholeRequestException(String.format("Invalid Ip: %s", ip));
      }
   }

   @RequestMapping(value = "/vrojobs", method = RequestMethod.GET)
   public List<JobConfig> getVROJobs() {
      return Lists.newArrayList(jobsRepository.findAllByJobType(JobType.VRO.name()));
   }

   @RequestMapping(value = "/vcjobs", method = RequestMethod.GET)
   public List<JobConfig> getVCJobs() {
      return Lists.newArrayList(jobsRepository.findAllByJobType(JobType.VCENTER.name()));
   }

   @RequestMapping(value = "/type/{jobtype}", method = RequestMethod.GET)
   public List<JobConfig> getJobsByType(@PathVariable("jobtype") JobType type) {
      return Lists.newArrayList(jobsRepository.findAllByJobType(type.name()));
   }

}
