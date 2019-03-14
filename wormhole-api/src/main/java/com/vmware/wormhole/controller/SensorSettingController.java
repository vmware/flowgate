/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.controller;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vmware.wormhole.common.WormholeConstant;
import com.vmware.wormhole.common.model.SensorSetting;
import com.vmware.wormhole.exception.WormholeRequestException;
import com.vmware.wormhole.repository.SensorSettingRepository;

@RestController
@RequestMapping(value = "/v1/sensors")
public class SensorSettingController {

   @Autowired
   private SensorSettingRepository repository;

   // Create
   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value="/setting",method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void createSensorSetting(@RequestBody SensorSetting sensorSetting) {
      SensorSetting example = new SensorSetting();
      example.setType(sensorSetting.getType());
      if(!repository.findAll(Example.of(example)).isEmpty()) {
         throw new WormholeRequestException("Duplicate sensor setting.");
      }
      repository.save(sensorSetting);
   }

   @RequestMapping(value="setting",method = RequestMethod.GET)
   public List<SensorSetting> getSensorSettings() {
      return repository.findAll();
   }
   //get
   @RequestMapping(value="setting/{id}",method = RequestMethod.GET)
   public SensorSetting getSensorSetting(@PathVariable("id") String id) {

      return repository.findOne(id);
   }

   //get
   @RequestMapping(value="/setting/page/{pageNumber}/pagesize/{pageSize}",method = RequestMethod.GET)
   public Page<SensorSetting> getSensorSettingsByPage(
         @PathVariable("pageNumber") int pageNumber,
         @PathVariable("pageSize") int pageSize) {
      if(pageNumber < 1) {
         pageNumber = 1;
      }else if(pageSize == 0) {
         pageSize = WormholeConstant.defaultPageSize;
      }else if(pageSize > WormholeConstant.maxPageSize) {
         pageSize = WormholeConstant.maxPageSize;
      }
      try{
         PageRequest pageRequest = new PageRequest(pageNumber-1,pageSize);
         return repository.findAll(pageRequest);
      }catch(Exception e) {
         e.printStackTrace();
         throw new WormholeRequestException(e.getMessage());
      }
   }

   //Update
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value="/setting",method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateSensorSetting(@RequestBody SensorSetting sensorSetting) {
      SensorSetting old = repository.findOne(sensorSetting.getId());
      if (old == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND, "SensorSetting not found", null);
      }
      HashMap<String, Object> changedFileds = null;
      try {
         changedFileds = queryChangedSensorSettingFileds(old, sensorSetting);
      } catch (Exception e) {
         throw new WormholeRequestException("Failed to update the Sensor setting.", e);
      }
      if (changedFileds.isEmpty()) {
         throw new WormholeRequestException(HttpStatus.OK,"Nothing is modified",null);
      }
      repository.updateSensorSettingByFileds(sensorSetting.getId(),changedFileds);
   }
   // Delete
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/setting/{id}", method = RequestMethod.DELETE)
   public void delete(@PathVariable String id) {
      repository.delete(id);
   }

   private HashMap<String, Object> queryChangedSensorSettingFileds(SensorSetting oldSensorSetting,
         SensorSetting newSensorSetting) throws NoSuchFieldException, IllegalAccessException,
         SecurityException, JsonProcessingException {
      HashMap<String, Object> changes = new HashMap<String, Object>();
      Class<?> oldC = oldSensorSetting.getClass();
      for (Field fieldNew : newSensorSetting.getClass().getDeclaredFields()) {
         fieldNew.setAccessible(true);
         Field fieldOld = oldC.getDeclaredField(fieldNew.getName());
         fieldOld.setAccessible(true);
         Object newValue = fieldNew.get(newSensorSetting);
         Object oldValue = fieldOld.get(oldSensorSetting);
         if (null != newValue) {
            if (fieldNew.get(newSensorSetting).equals(fieldOld.get(oldSensorSetting))) {
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
}
