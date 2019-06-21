/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.WormholeUser;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.repository.UserRepository;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.service.ServerValidationService;
import com.vmware.flowgate.util.BaseDocumentUtil;
import com.vmware.flowgate.util.EncryptionGuard;
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

   @Autowired
   private UserRepository userRepository;

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
      if (repository.findOneByServerURL(config.getServerURL()) != null) {
         String message = String.format("The server %s is already exsit.", config.getServerURL());
         throw new WormholeRequestException(message);
      }
      serverValidationService.validateFacilityServer(config);
      IntegrationStatus integrationStatus = new IntegrationStatus();
      integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
      integrationStatus.setStatus(IntegrationStatus.Status.ACTIVE);
      config.setIntegrationStatus(integrationStatus);
      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      config.setUserId(user.getUserId());
      encryptServerPassword(config);
      BaseDocumentUtil.generateID(config);
      repository.save(config);
      decryptServerPassword(config);
      notifyFacilityWorker(config);
   }

   @RequestMapping(value = "/{id}", method = RequestMethod.GET)
   public FacilitySoftwareConfig getFacilitySoftwareConfigByID(@PathVariable String id) {
      FacilitySoftwareConfig server = repository.findOne(id);
      decryptServerPassword(server);
      return server;
   }

   @RequestMapping(value = "/type/{type}", method = RequestMethod.GET)
   public List<FacilitySoftwareConfig> getFacilitySoftwareConfigByType(
         @PathVariable SoftwareType type) {
      List<FacilitySoftwareConfig> result = repository.findAllByType(type.name());
      if (result != null) {
         decryptServerListPassword(result);
      }
      return result;
   }

   @RequestMapping(value = "/page/{pageNumber}/pagesize/{pageSize}", method = RequestMethod.GET)
   public Page<FacilitySoftwareConfig> queryFacilitySoftwareConfigByPage(
         @PathVariable("pageNumber") int currentPage, @PathVariable("pageSize") int pageSize,
         HttpServletRequest request) {

      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      if (currentPage < FlowgateConstant.defaultPageNumber) {
         currentPage = FlowgateConstant.defaultPageNumber;
      } else if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = new PageRequest(currentPage - 1, pageSize);
      WormholeUser currentUser = userRepository.findOne(user.getUserId());
      Page<FacilitySoftwareConfig> result = null;
      if (currentUser.getRoleNames().contains(FlowgateConstant.Role_admin)) {
         result =  repository.findAll(pageRequest);
         decryptServerListPassword(result.getContent());
      } else {
         result = repository.findALlByUserId(user.getUserId(), pageRequest);
         decryptServerListPassword(result.getContent());
      }
      return result;
   }

   // Delete
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
   public void delete(@PathVariable String id) {
      repository.delete(id);
   }

   //only modify the status of integration,and not verify information of server.
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/status", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateStatus(@RequestBody FacilitySoftwareConfig server) {
      FacilitySoftwareConfig old = repository.findOne(server.getId());
      if (old == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND,
               "FacilitySoftwareConfig not found", null);
      }
      old.setIntegrationStatus(server.getIntegrationStatus());
      repository.save(old);
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
      old.setName(config.getName());
      old.setDescription(config.getDescription());
      old.setUserName(config.getUserName());
      old.setPassword(config.getPassword());
      old.setVerifyCert(config.isVerifyCert());
      old.setAdvanceSetting(config.getAdvanceSetting());
      old.setIntegrationStatus(config.getIntegrationStatus());
      serverValidationService.validateFacilityServer(config);
      encryptServerPassword(old);
      repository.save(old);
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/syncdatabyserverid/{id}", method = RequestMethod.POST)
   public void syncFacilityServerData(@PathVariable("id") String id, HttpServletRequest request) {
      FacilitySoftwareConfig server = repository.findOne(id);
      if (server == null) {
         throw new WormholeRequestException("Invalid ID");
      }
      decryptServerPassword(server);
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
            template.opsForList().leftPushAll(EventMessageUtil.powerIQJobList,
                  EventMessageUtil.generateFacilityMessageListByType(EventType.PowerIQ,
                        EventMessageUtil.PowerIQ_SyncAllPDUID,
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

   private void encryptServerPassword(FacilitySoftwareConfig server) {
      if (server.getPassword() == null || server.getPassword().equals("")) {
         return;
      } else {
         try {
            server.setPassword(EncryptionGuard.encode(server.getPassword()));
         } catch (UnsupportedEncodingException e) {
            throw new WormholeException(e.getMessage(), e.getCause());
         } catch (GeneralSecurityException e) {
            throw new WormholeException(e.getMessage(), e.getCause());
         }
      }
   }

   private void decryptServerPassword(FacilitySoftwareConfig server) {
      if (server.getPassword() == null || server.getPassword().equals("")) {
         return;
      } else {
         try {
            server.setPassword(EncryptionGuard.decode(server.getPassword()));
         } catch (UnsupportedEncodingException e) {
            throw new WormholeException(e.getMessage(), e.getCause());
         } catch (GeneralSecurityException e) {
            throw new WormholeException(e.getMessage(), e.getCause());
         }
      }
   }

   private void decryptServerListPassword(List<FacilitySoftwareConfig> servers) {
      for (FacilitySoftwareConfig server : servers) {
         decryptServerPassword(server);
      }
   }
}
