/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.AdapterJobCommand;
import com.vmware.flowgate.common.model.FacilityAdapter;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.FacilityAdapterRepository;
import com.vmware.flowgate.util.BaseDocumentUtil;

@RestController
@RequestMapping("/v1/facilityadapter")
public class FacilityAdapterController {

   @Autowired
   private FacilityAdapterRepository facilityAdapterRepo;
   private static final String QUEUE_NAME_SUFFIX = ":joblist";
   private static final String JOIN_FLAG = "_";
   private static Set<String> predefineName = new HashSet<String>();
   static {
      predefineName.add("Nlyte");
      predefineName.add("PowerIQ");
      predefineName.add("Device42");
      predefineName.add("InfoBlox");
      predefineName.add("Labsdb");
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public HttpHeaders createAdapterType(@RequestBody FacilityAdapter adapter) {
      String displayName = adapter.getDisplayName();
      if(displayName == null || predefineName.contains(displayName)) {
         throw WormholeRequestException.InvalidFiled("DisplayName", displayName);
      }
      FacilityAdapter oldAdapter = facilityAdapterRepo.findByDisplayName(displayName);
      if(oldAdapter != null) {
         throw new WormholeRequestException("Adapter with dispalyName : "+displayName+" is existed");
      }
      List<AdapterJobCommand> commands = adapter.getCommands();
      if(commands == null || commands.isEmpty()) {
         throw new WormholeRequestException("The Commands field is required.");
      }
      HttpHeaders httpHeaders = new HttpHeaders();
      BaseDocumentUtil.generateID(adapter);
      String unique_value = adapter.getType().name()+ JOIN_FLAG + UUID.randomUUID().toString().replaceAll("-", "");
      adapter.setTopic(unique_value);
      adapter.setSubCategory(unique_value);
      adapter.setQueueName(adapter.getSubCategory() + QUEUE_NAME_SUFFIX);
      facilityAdapterRepo.save(adapter);
      httpHeaders.setLocation(linkTo(AssetController.class).slash(adapter.getId()).toUri());
      return httpHeaders;
   }

   @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateAdapterType(@RequestBody FacilityAdapter type) {
      FacilityAdapter equalNameAdapter = facilityAdapterRepo.findByDisplayName(type.getDisplayName());
      if(equalNameAdapter != null && !equalNameAdapter.getId().equals(type.getId())) {
         throw new WormholeRequestException("Adapter with dispalyName : "+type.getDisplayName()+" is existed");
      }
      List<AdapterJobCommand> commands = type.getCommands();
      if(commands == null || commands.isEmpty()) {
         throw new WormholeRequestException("The Commands field is required.");
      }
      Optional<FacilityAdapter> oldAdapterTypeOptional = facilityAdapterRepo.findById(type.getId());
      if(!oldAdapterTypeOptional.isPresent()) {
         throw WormholeRequestException.NotFound("FacilityAdapter", "id", type.getId());
      }
      FacilityAdapter oldAdapterType = oldAdapterTypeOptional.get();
      oldAdapterType.setDescription(type.getDescription());
      oldAdapterType.setDisplayName(type.getDisplayName());
      oldAdapterType.setCommands(commands);
      facilityAdapterRepo.save(type);
   }

   @RequestMapping(value = "/{id}", method = RequestMethod.GET)
   public FacilityAdapter read(@PathVariable String id) {
      Optional<FacilityAdapter> typeOptional = facilityAdapterRepo.findById(id);
      return typeOptional.get();
   }

   @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
   public void remove(@PathVariable String id) {
      facilityAdapterRepo.deleteById(id);
   }

   @RequestMapping(value = "/pagenumber/{pagenumber}/pagesize/{pagesize}", method = RequestMethod.GET)
   public Page<FacilityAdapter> findAllAdapter(@PathVariable("pagenumber") int currentPage,
         @PathVariable("pagesize") int pageSize){
      if (currentPage < FlowgateConstant.defaultPageNumber) {
         currentPage = FlowgateConstant.defaultPageNumber;
      } else if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize,Direction.ASC,"createTime");
      return facilityAdapterRepo.findAll(pageRequest);
   }
}
