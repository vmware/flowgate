/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import java.util.List;

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
import com.vmware.flowgate.service.FacilityAdapterService;

@RestController
@RequestMapping("/v1/facilityadapter")
public class FacilityAdapterController {

   @Autowired
   private FacilityAdapterRepository facilityAdapterRepo;

   @Autowired
   private FacilityAdapterService facilityAdapterService;

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public HttpHeaders createAdapterType(@RequestBody FacilityAdapter adapter) {
      return facilityAdapterService.create(adapter);
   }

   @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateAdapterType(@RequestBody FacilityAdapter adapter) {
      FacilityAdapter equalNameAdapter = facilityAdapterRepo.findByDisplayName(adapter.getDisplayName());
      if(equalNameAdapter != null && !equalNameAdapter.getId().equals(adapter.getId())) {
         throw new WormholeRequestException("Adapter with dispalyName : "+adapter.getDisplayName()+" is existed");
      }
      List<AdapterJobCommand> commands = adapter.getCommands();
      if(commands == null || commands.isEmpty()) {
         throw new WormholeRequestException("The Commands field is required.");
      }
      FacilityAdapter oldAdapterType = facilityAdapterService.findById(adapter.getId());
      oldAdapterType.setDescription(adapter.getDescription());
      oldAdapterType.setDisplayName(adapter.getDisplayName());
      oldAdapterType.setCommands(commands);
      facilityAdapterRepo.save(adapter);
   }

   @RequestMapping(value = "/{id}", method = RequestMethod.GET)
   public FacilityAdapter read(@PathVariable String id) {
      return facilityAdapterService.findById(id);
   }

   @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
   public void remove(@PathVariable String id) {
      facilityAdapterService.deleteAdapter(id);
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

   @RequestMapping(method = RequestMethod.GET)
   public List<FacilityAdapter> findAll(){
      return facilityAdapterRepo.findAllFacilityAdapters();
   }

}
