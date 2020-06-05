/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.SensorSetting;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.SensorSettingRepository;
import com.vmware.flowgate.util.BaseDocumentUtil;

@RestController
@RequestMapping(value = "/v1/sensors")
public class SensorSettingController {

   @Autowired
   private SensorSettingRepository repository;

   // Create
   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value="/setting",method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void createSensorSetting(@RequestBody SensorSetting sensorSetting) {
      if (!repository.findAllByType(sensorSetting.getType()).isEmpty()) {
         throw new WormholeRequestException("Duplicate sensor setting.");
      }
      BaseDocumentUtil.generateID(sensorSetting);
      repository.save(sensorSetting);
   }

   //get
   @RequestMapping(value="/setting/{id}",method = RequestMethod.GET)
   public SensorSetting getSensorSetting(@PathVariable("id") String id) {
      Optional<SensorSetting> sensorSettingOptional = repository.findById(id);
      return sensorSettingOptional.get();
   }

   //get
   @RequestMapping(value="/setting/page/{pageNumber}/pagesize/{pageSize}",method = RequestMethod.GET)
   public Page<SensorSetting> getSensorSettingsByPage(
         @PathVariable("pageNumber") int pageNumber,
         @PathVariable("pageSize") int pageSize) {
      if(pageNumber < FlowgateConstant.defaultPageNumber) {
         pageNumber = FlowgateConstant.defaultPageNumber;
      }else if(pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      }else if(pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      try{
         PageRequest pageRequest = PageRequest.of(pageNumber-1,pageSize);
         return repository.findAll(pageRequest);
      }catch(Exception e) {
         throw new WormholeRequestException(e.getMessage());
      }
   }

   //Update
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value="/setting",method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateSensorSetting(@RequestBody SensorSetting sensorSetting) {
      Optional<SensorSetting> oldSensorSettingOptional = repository.findById(sensorSetting.getId());
      if(!oldSensorSettingOptional.isPresent()) {
         throw WormholeRequestException.NotFound("SensorSetting", "id", sensorSetting.getId());
      }
      SensorSetting old = oldSensorSettingOptional.get();
      try {
         BaseDocumentUtil.applyChanges(old, sensorSetting);
      } catch (Exception e) {
         throw new WormholeRequestException("Failed to update the Sensor setting.", e);
      }
      repository.save(old);
   }

   // Delete
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/setting/{id}", method = RequestMethod.DELETE)
   public void delete(@PathVariable String id) {
      repository.deleteById(id);
   }

}
