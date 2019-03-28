/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.flowgate.common.WormholeConstant;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.service.ServerValidationService;
import com.vmware.flowgate.util.HandleURL;
import com.vmware.flowgate.util.WormholeUserDetails;

@RestController
@RequestMapping(value = "/v1/facilitysoftware")
public class FacilitySoftwareController {

   @Autowired
   private FacilitySoftwareConfigRepository repository;

   @Autowired
   private ServerValidationService serverValidationService;

   @Autowired
   private AccessTokenService accessTokenService;

   @Autowired
   private MessagePublisher publisher;

   private static final Logger logger = LoggerFactory.getLogger(FacilitySoftwareController.class);
   @Autowired
   private StringRedisTemplate template;

   //create a new facilitySoftwareConfig
   @RequestMapping(method = RequestMethod.POST)
   @ResponseStatus(HttpStatus.CREATED)
   public void createServer(@RequestBody FacilitySoftwareConfig config,
         HttpServletRequest request) {
      HandleURL handleURL = new HandleURL();
      config.setServerURL(handleURL.formatURL(config.getServerURL()));
      FacilitySoftwareConfig facility = new FacilitySoftwareConfig();
      facility.setServerURL(config.getServerURL());
      ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("verifyCert");
      Example<FacilitySoftwareConfig> example = Example.of(facility, matcher);
      if (repository.findOne(example) != null) {
         String message = String.format("The server %s is already exsit.", config.getServerURL());
         throw new WormholeRequestException(message);
      }
      serverValidationService.validateFacilityServer(config);
      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      config.setUserId(user.getUserId());
      repository.save(config);
      notifyFacilityWorker(config);
   }

   @RequestMapping(value = "/{id}", method = RequestMethod.GET)
   public FacilitySoftwareConfig getFacilitySoftwareConfigByID(@PathVariable String id) {
      return repository.findOne(id);
   }

   @RequestMapping(value = "/type/{type}", method = RequestMethod.GET)
   public List<FacilitySoftwareConfig> getFacilitySoftwareConfigByType(
         @PathVariable SoftwareType type) {
      FacilitySoftwareConfig facility = new FacilitySoftwareConfig();
      facility.setType(type);
      ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("verifyCert");
      Example<FacilitySoftwareConfig> example = Example.of(facility, matcher);
      return repository.findAll(example);
   }

   @RequestMapping(value = "/page/{pageNumber}/pagesize/{pageSize}", method = RequestMethod.GET)
   public Page<FacilitySoftwareConfig> queryFacilitySoftwareConfigByPage(
         @PathVariable("pageNumber") int currentPage, @PathVariable("pageSize") int pageSize,
         HttpServletRequest request) {
      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      if (currentPage < 1) {
         currentPage = 1;
      } else if (pageSize == 0) {
         pageSize = WormholeConstant.defaultPageSize;
      } else if (pageSize > WormholeConstant.maxPageSize) {
         pageSize = WormholeConstant.maxPageSize;
      }
      try {

         PageRequest pageRequest = new PageRequest(currentPage - 1, pageSize);
         FacilitySoftwareConfig facility = new FacilitySoftwareConfig();
         facility.setUserId(user.getUserId());
         ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("verifyCert");
         Example<FacilitySoftwareConfig> example = Example.of(facility, matcher);
         return repository.findAll(example, pageRequest);
      } catch (Exception e) {
         e.printStackTrace();
         throw new WormholeRequestException(e.getMessage());
      }
   }

   // Delete
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
   public void delete(@PathVariable String id) {
      repository.delete(id);
   }

   //Update
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateFacilitySoftwareConfig(@RequestBody FacilitySoftwareConfig config) {
      FacilitySoftwareConfig old = repository.findOne(config.getId());
      if (old == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND,
               "FacilitySoftwareConfig not found", null);
      }
      try {
         old.setName(config.getName());
         old.setDescription(config.getDescription());
         old.setUserName(config.getUserName());
         old.setPassword(config.getPassword());
         old.setVerifyCert(config.isVerifyCert());
         old.setAdvanceSetting(config.getAdvanceSetting());
         old.setIntegrationStatus(config.getIntegrationStatus());
         serverValidationService.validateFacilityServer(config);
         /**If the integrationStatus is error and passed the server validation,
         then update the status from error to active.**/
         if(config.getIntegrationStatus().getStatus().equals(IntegrationStatus.Status.ERROR)) {
            IntegrationStatus status = new IntegrationStatus();
            status.setStatus(IntegrationStatus.Status.ACTIVE);
            old.setIntegrationStatus(status);
         }
         repository.save(old);
      } catch (Exception e) {
         throw new WormholeRequestException(e.getMessage());
      }
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/syncdatabyserverid/{id}", method = RequestMethod.POST)
   public void syncFacilityServerData(@PathVariable("id") String id, HttpServletRequest request) {
      FacilitySoftwareConfig example = new FacilitySoftwareConfig();
      example.setId(id);
      FacilitySoftwareConfig server = repository.findOne(Example.of(example));
      if (server == null) {
         throw new WormholeRequestException("Invalid ID");
      }
      notifyFacilityWorker(server);
   }

   private void notifyFacilityWorker(FacilitySoftwareConfig server) {
      switch (server.getType()) {
      case PowerIQ:
         try {
            logger.info(
                  String.format("Notify %s worker to start sync data, job queue:%s, notifytopic:%s",
                        server.getType(), EventMessageUtil.powerIQJobList,
                        EventMessageUtil.POWERIQTopic));
            template.opsForList().leftPushAll(EventMessageUtil.powerIQJobList,
                  EventMessageUtil.generateFacilityMessageListByType(EventType.PowerIQ,
                        EventMessageUtil.PowerIQ_SyncSensorMetaData,
                        new FacilitySoftwareConfig[] { server }));
            publisher.publish(EventMessageUtil.POWERIQTopic,
                  EventMessageUtil.generateFacilityNotifyMessage(EventType.PowerIQ));
            logger.info("Notify message sent out.");
         } catch (IOException e) {
            logger.error("Failed to send out message", e);
         }
         break;
      case Nlyte:
         try {
            logger.info(String.format(
                  "Notify %s worker to start sync data, job queue:%s, notifytopic:%s",
                  server.getType(), EventMessageUtil.nlyteJobList, EventMessageUtil.NLYTETOPIC));
            template.opsForList().leftPushAll(EventMessageUtil.nlyteJobList,
                  EventMessageUtil.generateFacilityMessageListByType(EventType.Nlyte,
                        EventMessageUtil.NLYTE_SyncAllAssets,
                        new FacilitySoftwareConfig[] { server }));
            publisher.publish(EventMessageUtil.NLYTETOPIC,
                  EventMessageUtil.generateFacilityNotifyMessage(EventType.Nlyte));
            logger.info("Notify message sent out.");
         } catch (IOException e) {
            logger.error("Failed to send out message", e);
         }
         break;
      case Labsdb:
         try {
            logger.info(String.format(
                  "Notify %s worker to start sync data, job queue:%s, notifytopic:%s",
                  server.getType(), EventMessageUtil.labsdbJobList, EventMessageUtil.LabsdbTopic));
            template.opsForList().leftPushAll(EventMessageUtil.labsdbJobList,
                  EventMessageUtil.generateFacilityMessageListByType(EventType.Labsdb,
                        EventMessageUtil.Labsdb_SyncAllWireMapData,
                        new FacilitySoftwareConfig[] { server }));
            publisher.publish(EventMessageUtil.LabsdbTopic,
                  EventMessageUtil.generateFacilityNotifyMessage(EventType.Labsdb));
            logger.info("Notify message sent out.");
         } catch (IOException e) {
            logger.error("Failed to send out message", e);
         }
         break;
      default:
         break;
      }
   }
}
