/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.config.InitializeConfigureData;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.SDDCSoftwareRepository;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.service.ServerValidationService;
import com.vmware.flowgate.util.WormholeUserDetails;

@RestController
@RequestMapping(value = "/v1/sddc")
public class SDDCSoftwareController {

   @Autowired
   protected SDDCSoftwareRepository sddcRepository;

   @Autowired
   private ServerValidationService serverValidationService;

   @Autowired
   private AccessTokenService accessTokenService;

   @Autowired
   private MessagePublisher publisher;

   @Autowired
   private StringRedisTemplate template;

   private static final Logger logger = LoggerFactory.getLogger(SDDCSoftwareController.class);

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(method = RequestMethod.POST)
   public void createServer(@RequestBody SDDCSoftwareConfig server, HttpServletRequest request) {

      String ip = server.getServerURL();
      SDDCSoftwareConfig example = new SDDCSoftwareConfig();
      example.setServerURL(ip);
      example = sddcRepository.findOne(Example.of(example));
      if (example != null) {
         String message = String.format("The server %s is already exsit.", ip);
         throw new WormholeRequestException(message);
      }
      switch (server.getType()) {
      case VRO:
         serverValidationService.validateVROServer(server);
         break;
      case VCENTER:
         serverValidationService.validVCServer(server);
         break;
      default:
         throw WormholeRequestException.InvalidFiled("type", server.getType().toString());
      }
      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      server.setUserId(user.getUserId());
      IntegrationStatus integrationStatus = new IntegrationStatus();
      integrationStatus.setRetryCounter(FlowgateConstant.DEFAULTNUMBEROFRETRIES);
      integrationStatus.setStatus(IntegrationStatus.Status.ACTIVE);
      server.setIntegrationStatus(integrationStatus);
      sddcRepository.save(server);
      //notify worker for the start jobs
      notifySDDCWorker(server);
   }

   // Delete
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
   public void delete(@PathVariable String id) {
      sddcRepository.delete(id);
   }

   //Update
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateSDDCSoftwareConfig(@RequestBody SDDCSoftwareConfig server,HttpServletRequest request) {
      SDDCSoftwareConfig old = sddcRepository.findOne(server.getId());
      if (old == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND, "SDDCSoftware not found", null);
      }
      HashMap<String, Object> changedFileds = null;
      try {
         server.setServerURL(old.getServerURL());
         server.setType(old.getType());
         server.setUserId(old.getUserId());
         changedFileds = queryChangedSDDCSoftFileds(old, server);
      } catch (Exception e) {
         throw new WormholeRequestException("Faild to update the SDDCSoftware", e);
      }
      if (changedFileds.isEmpty()) {
         throw new WormholeRequestException(HttpStatus.OK, "Nothing is modified", null);
      }
      if(!InitializeConfigureData.checkServiceKey(request.getHeader("servicekey"))) {
         switch (server.getType()) {
         case VRO:
            serverValidationService.validateVROServer(server);
            break;
         case VCENTER:
            serverValidationService.validVCServer(server);
            break;
         default:
            throw WormholeRequestException.InvalidFiled("type", server.getType().toString());
         }
      }
      sddcRepository.updateSDDCSoftwareByFileds(server.getId(), changedFileds);
      
   }


   private HashMap<String, Object> queryChangedSDDCSoftFileds(SDDCSoftwareConfig oldServer,
         SDDCSoftwareConfig newServer) throws NoSuchFieldException, IllegalAccessException,
         SecurityException, JsonProcessingException {
      HashMap<String, Object> changes = new HashMap<String, Object>();
      Class<?> oldC = oldServer.getClass();
      for (Field fieldNew : newServer.getClass().getDeclaredFields()) {
         fieldNew.setAccessible(true);
         Field fieldOld = oldC.getDeclaredField(fieldNew.getName());
         fieldOld.setAccessible(true);
         Object newValue = fieldNew.get(newServer);
         Object oldValue = fieldOld.get(oldServer);
         if (null != newValue) {
            if (fieldNew.get(newServer).equals(fieldOld.get(oldServer))) {
               continue;
            }
         } else {
            if (null == oldValue) {
               continue;
            }
         }
         changes.put(fieldOld.getName(), newValue);
      }
      return changes;
   }

   //get a server
   @RequestMapping(value = "/{id}", method = RequestMethod.GET)
   public SDDCSoftwareConfig getServerConfig(@PathVariable String id) {
      return sddcRepository.findOne(id);
   }

   //get
   @RequestMapping(value = "/vrops", method = RequestMethod.GET)
   public List<SDDCSoftwareConfig> getVROServerConfigs() {
      SDDCSoftwareConfig example = new SDDCSoftwareConfig();
      example.setType(SoftwareType.VRO);
      return sddcRepository.findAll(Example.of(example));
   }

   @RequestMapping(value = "/vc", method = RequestMethod.GET)
   public List<SDDCSoftwareConfig> getVCServerConfigs() {
      SDDCSoftwareConfig example = new SDDCSoftwareConfig();
      example.setType(SoftwareType.VCENTER);
      return sddcRepository.findAll(Example.of(example));
   }

   @RequestMapping(value = "/page/{pageNumber}/pagesize/{pageSize}", method = RequestMethod.GET)
   public Page<SDDCSoftwareConfig> queryServer(@PathVariable("pageNumber") int currentPage,
         @PathVariable("pageSize") int pageSize, HttpServletRequest request) {
      if (currentPage < 1) {
         currentPage = 1;
      } else if (pageSize == 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = new PageRequest(currentPage - 1, pageSize);
      SDDCSoftwareConfig sddc = new SDDCSoftwareConfig();
      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      sddc.setUserId(user.getUserId());
      try {
         ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("verifyCert");
         Example<SDDCSoftwareConfig> example = Example.of(sddc, matcher);
         return sddcRepository.findAll(example, pageRequest);
      } catch (Exception e) {
         throw new WormholeRequestException(e.getMessage());
      }
   }

   //get servers by user
   @RequestMapping(value = "/user/vrops", method = RequestMethod.GET)
   public List<SDDCSoftwareConfig> getVROServerConfigsByUser(HttpServletRequest request) {

      SDDCSoftwareConfig example = new SDDCSoftwareConfig();
      example.setUserId(getCurrentUserID(request));
      List<SDDCSoftwareConfig> datas = sddcRepository.findAll(Example.of(example));
      if (datas.isEmpty()) {
         throw new WormholeRequestException("The result is empty");
      }
      return datas;
   }

   //get servers by user and type
   @RequestMapping(value = "/type/{type}", method = RequestMethod.GET)
   public List<SDDCSoftwareConfig> getServerConfigsByUser(@PathVariable("type") SoftwareType type,
         HttpServletRequest request) {

      SDDCSoftwareConfig example = new SDDCSoftwareConfig();
      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      example.setType(type);
      example.setUserId(user.getUserId());
      List<SDDCSoftwareConfig> datas = sddcRepository.findAll(Example.of(example));
      return datas;
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/syncdatabyserverid/{id}", method = RequestMethod.POST)
   public void syncSDDCServerData(@PathVariable("id") String id, HttpServletRequest request) {
      SDDCSoftwareConfig example = new SDDCSoftwareConfig();
      example.setId(id);
      example.setUserId(getCurrentUserID(request));
      SDDCSoftwareConfig server = sddcRepository.findOne(Example.of(example));
      notifySDDCWorker(server);
   }

   private String getCurrentUserID(HttpServletRequest request) {
      return accessTokenService.getCurrentUser(request).getUserId();
   }

   private void notifySDDCWorker(SDDCSoftwareConfig server) {
      String jobList = null;
      String jobTarget1 = null;
      String jobTarget2 = null;
      String notifyTopic = null;
      EventType eventType = null;
      switch (server.getType()) {
      case VCENTER:
         jobList = EventMessageUtil.vcJobList;
         jobTarget1 = EventMessageUtil.VCENTER_SyncCustomerAttrs;
         jobTarget2 = EventMessageUtil.VCENTER_SyncCustomerAttrsData;
         notifyTopic = EventMessageUtil.VCTopic;
         eventType = EventType.VCenter;
         break;
      case VRO:
         jobList = EventMessageUtil.vroJobList;
         jobTarget1 = EventMessageUtil.VRO_SyncMetricPropertyAndAlert;
         jobTarget2 = EventMessageUtil.VRO_SyncMetricData;
         notifyTopic = EventMessageUtil.VROTopic;
         eventType = EventType.VROps;
         break;
      default:
         break;
      }
      try {
         logger.info(
               String.format("Notify %s worker to start sync data, job queue:%s, notifytopic:%s",
                     server.getType(), jobList, notifyTopic));
         template.opsForList().leftPushAll(jobList, EventMessageUtil.generateSDDCMessageListByType(
               eventType, jobTarget1, new SDDCSoftwareConfig[] { server }));
         template.opsForList().leftPushAll(jobList, EventMessageUtil.generateSDDCMessageListByType(
               eventType, jobTarget2, new SDDCSoftwareConfig[] { server }));
         publisher.publish(notifyTopic, EventMessageUtil.generateSDDCNotifyMessage(eventType));
      } catch (IOException e) {
         logger.error("Failed to send out message", e);
      }
   }
}
