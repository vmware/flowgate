/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.SDDCSoftwareRepository;
import com.vmware.flowgate.repository.ServerMappingRepository;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.service.ServerValidationService;
import com.vmware.flowgate.util.BaseDocumentUtil;
import com.vmware.flowgate.util.EncryptionGuard;
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
   
   @Autowired
   ServerMappingRepository serverMappingRepository;

   private static final Logger logger = LoggerFactory.getLogger(SDDCSoftwareController.class);

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(method = RequestMethod.POST)
   public void createServer(@RequestBody SDDCSoftwareConfig server, HttpServletRequest request) {

      String ip = server.getServerURL();
      SDDCSoftwareConfig example = sddcRepository.findOneByServerURL(ip);
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
      case VROPSMP:
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
      //encrypt the password
      encryptServerPassword(server);
      BaseDocumentUtil.generateID(server);
      sddcRepository.save(server);
      //notify worker for the start jobs
      decryptServerPassword(server);
      notifySDDCWorker(server);
   }

   // Delete
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
   public void delete(@PathVariable String id) {

	  Optional<SDDCSoftwareConfig> sddcOptional = sddcRepository.findById(id);
	  SDDCSoftwareConfig server = new SDDCSoftwareConfig();
	  try {
		  server = sddcOptional.get();
	  }catch(NoSuchElementException e) {
		  throw WormholeRequestException.NotFound("sddc", "id", id);
	  }
      
	  SoftwareType type = server.getType();
	  switch(type) {
	  case VCENTER:
		  List<ServerMapping> vcenterMappings = serverMappingRepository.findAllByVCID(id);
		  if(vcenterMappings != null && !vcenterMappings.isEmpty()) {
			  for(ServerMapping mapping : vcenterMappings) {
				  serverMappingRepository.deleteById(mapping.getId());
			  }
		  }
		  break;
	  case VRO:
	  case VROPSMP:
		  List<ServerMapping> mappings = serverMappingRepository.findAllByVroID(id);
		  if(mappings != null && !mappings.isEmpty()) {
			  for(ServerMapping mapping : mappings) {
				  serverMappingRepository.deleteById(mapping.getId());
			  }
		  }
		  break;
	  default:
		  logger.info(String.format("Integration Type: %s No Found.",
                  server.getType()));
		  break;
	  }
	  
	  sddcRepository.deleteById(id);
 
   }

   //only modify the status of integration,and not verify information of server.
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/status", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateStatus(@RequestBody SDDCSoftwareConfig server) {
      Optional<SDDCSoftwareConfig> oldSddcOptional = sddcRepository.findById(server.getId());
      if (!oldSddcOptional.isPresent()) {
         throw WormholeRequestException.NotFound("SDDCSoftwareConfig", "id", server.getId());
      }
      SDDCSoftwareConfig old = oldSddcOptional.get();
      old.setIntegrationStatus(server.getIntegrationStatus());
      sddcRepository.save(old);
   }

   //Update
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateSDDCSoftwareConfig(@RequestBody SDDCSoftwareConfig server) {
      Optional<SDDCSoftwareConfig> oldSddcOptional = sddcRepository.findById(server.getId());
      if (!oldSddcOptional.isPresent()) {
         throw WormholeRequestException.NotFound("SDDCSoftwareConfig", "id", server.getId());
      }
      SDDCSoftwareConfig old = oldSddcOptional.get();
      server.setServerURL(old.getServerURL());
      server.setType(old.getType());
      server.setUserId(old.getUserId());
      if (StringUtils.isBlank(server.getPassword())) {
         decryptServerPassword(old);
         server.setPassword(old.getPassword());
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
      encryptServerPassword(server);
      try {
         BaseDocumentUtil.applyChanges(old, server);
      } catch (Exception e) {
         throw new WormholeRequestException("Faild to update the SDDCSoftware", e);
      }
      sddcRepository.save(old);
   }

   //get a server
   @RequestMapping(value = "/{id}", method = RequestMethod.GET)
   public SDDCSoftwareConfig getServerConfig(@PathVariable String id) {
      Optional<SDDCSoftwareConfig> sddcOptional = sddcRepository.findById(id);
      SDDCSoftwareConfig server = null;
      if (sddcOptional.isPresent()) {
         server = sddcOptional.get();
         server.setPassword(null);
      }
      return server;
   }

   //get
   @RequestMapping(value = "/vrops", method = RequestMethod.GET)
   public List<SDDCSoftwareConfig> getVROServerConfigs() {
      List<SDDCSoftwareConfig> result = sddcRepository.findAllByType(SoftwareType.VRO.name());
      if (result != null) {
         for (SDDCSoftwareConfig sddcSoftwareConfig : result) {
            sddcSoftwareConfig.setPassword(null);
         }
      }
      return result;
   }

   @RequestMapping(value = "/vc", method = RequestMethod.GET)
   public List<SDDCSoftwareConfig> getVCServerConfigs() {
      List<SDDCSoftwareConfig> result = sddcRepository.findAllByType(SoftwareType.VCENTER.name());
      if (result != null) {
         for (SDDCSoftwareConfig sddcSoftwareConfig : result) {
            sddcSoftwareConfig.setPassword(null);
         }
      }
      return result;
   }

   @RequestMapping(value = "/page/{pageNumber}/pagesize/{pageSize}", method = RequestMethod.GET)
   public Page<SDDCSoftwareConfig> queryServer(@PathVariable("pageNumber") int currentPage,
         @PathVariable("pageSize") int pageSize, HttpServletRequest request) {
      if (currentPage < FlowgateConstant.defaultPageNumber) {
         currentPage = FlowgateConstant.defaultPageNumber;
      } else if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);
      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      try {
         Page<SDDCSoftwareConfig> result = sddcRepository.findAllByUserId(user.getUserId(), pageRequest);
         for (SDDCSoftwareConfig sddcSoftwareConfig : result.getContent()) {
            sddcSoftwareConfig.setPassword(null);
         }
         return result;
      } catch (Exception e) {
         throw new WormholeRequestException(e.getMessage());
      }
   }

   //get servers by user
   //Confuse, if we only filter out vrops why not set the type field?
   @RequestMapping(value = "/user/vrops", method = RequestMethod.GET)
   public List<SDDCSoftwareConfig> getVROServerConfigsByUser(HttpServletRequest request) {

      SDDCSoftwareConfig example = new SDDCSoftwareConfig();
      example.setUserId(getCurrentUserID(request));
      List<SDDCSoftwareConfig> datas = sddcRepository.findAllByUserId(getCurrentUserID(request));
      if (datas.isEmpty()) {
         throw new WormholeRequestException("The result is empty");
      }
      decryptServerListPassword(datas);
      return datas;
   }

   //get servers by user and type
   @RequestMapping(value = "/type/{type}", method = RequestMethod.GET)
   public List<SDDCSoftwareConfig> getServerConfigsByUser(@PathVariable("type") SoftwareType type,
         HttpServletRequest request) {
      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      List<SDDCSoftwareConfig> datas =
            sddcRepository.findAllByUserIdAndType(user.getUserId(), type.name());
      for (SDDCSoftwareConfig sddcSoftwareConfig : datas) {
         sddcSoftwareConfig.setPassword(null);
      }
      return datas;
   }

   @RequestMapping(value = "/internal/type/{type}", method = RequestMethod.GET)
   public List<SDDCSoftwareConfig> getInternalServerConfigsByUser(@PathVariable("type") SoftwareType type) {
      List<SDDCSoftwareConfig> result = sddcRepository.findAllByType(type.name());
      decryptServerListPassword(result);
      return result;
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/syncdatabyserverid/{id}", method = RequestMethod.POST)
   public void syncSDDCServerData(@PathVariable("id") String id, HttpServletRequest request) {
      Optional<SDDCSoftwareConfig> sddcOptional = sddcRepository.findById(id);
      SDDCSoftwareConfig server = sddcOptional.get();
      String userID =  getCurrentUserID(request);
      if(!userID.equals(server.getUserId())){
         return;
      }
      decryptServerPassword(server);
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
         return;
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

   private void encryptServerPassword(SDDCSoftwareConfig server) {
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

   private void decryptServerPassword(SDDCSoftwareConfig server) {
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

   private void decryptServerListPassword(List<SDDCSoftwareConfig> servers) {
      for (SDDCSoftwareConfig server : servers) {
         decryptServerPassword(server);
      }
   }
}
