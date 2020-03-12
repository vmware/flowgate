/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.aggregator;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.aggregator.scheduler.job.AggregatorService;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;

import junit.framework.TestCase;

public class MessageProcessingTest {

   @Autowired
   private StringRedisTemplate template;

   @Spy
   @InjectMocks
   private AggregatorService aggregatorService = new AggregatorService();

   @Test
   public void testMessage() {
      ObjectMapper mapper = new ObjectMapper();
      String template = "{\"type\":\"InfoBlox\",\"eventUser\":null,\"source\":null,\"target\":null,\"createTime\":%s,\"content\":\"nihao\"}";
      long time = new Date().getTime();
      EventMessage message =
            new EventMessageImpl(
                  EventType.InfoBlox, null, null, null, time, "nihao");
      String bb;
      try {
      bb = mapper.writeValueAsString(message);
      Assert.assertEquals(String.format(template, time), bb);

      EventMessage mess2 = mapper.readValue(bb, EventMessageImpl.class);
      Assert.assertEquals("nihao",mess2.getContent());

      String gg = "{\"type\":\"InfoBlox\",\"createTime\":1539073715966,\"content\":\"helloworld\"}";
      EventMessage mess3 = mapper.readValue(gg, EventMessageImpl.class);
      Assert.assertEquals("helloworld", mess3.getContent());
      }catch(IOException e) {
        Assert.fail();
      }
   }

   @Test
   public void testVC() throws Exception{
      ObjectMapper mapper = new ObjectMapper();
      SDDCSoftwareConfig vc = new SDDCSoftwareConfig();
      vc.setDescription("good vc");
      vc.setName("Test VC");
      vc.setServerURL("10.10.10.10");
      vc.setPassword("fake password");
      vc.setType(SoftwareType.VCENTER);
      String payload = mapper.writeValueAsString(vc);
//      EventMessage message =
//            EventMessageUtil.createEventMessage(EventType.VCenter, EventMessageUtil.VCENTER_SyncCustomerAttrs, payload);
      EventMessage message = EventMessageUtil.createEventMessage(EventType.VCenter, EventMessageUtil.VCENTER_SyncData, "");
      System.out.println(mapper.writeValueAsString(message));
   }

   @Test
   public void testGetPositionInfo() {
      Asset asset1 = new Asset();
      TestCase.assertEquals(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION, aggregatorService.getPositionInfo(asset1));

      Asset asset2 = new Asset();
      asset2.setCabinetUnitPosition(2);
      TestCase.assertEquals(FlowgateConstant.RACK_UNIT_PREFIX + asset2.getCabinetUnitPosition(), aggregatorService.getPositionInfo(asset2));


      Asset asset3 = new Asset();
      asset3.setCabinetUnitPosition(3);
      HashMap<String,String> sensorAssetJustfication = new HashMap<String, String>();
      Map<String,String> sensorInfo = new HashMap<String,String>();
      sensorInfo.put(FlowgateConstant.POSITION, "INLET");
      ObjectMapper mapper = new ObjectMapper();

      try {
         sensorAssetJustfication.put(FlowgateConstant.SENSOR, mapper.writeValueAsString(sensorInfo));
         asset3.setJustificationfields(sensorAssetJustfication);
      } catch (JsonProcessingException e) {
         TestCase.fail();
      }

      TestCase.assertEquals(FlowgateConstant.RACK_UNIT_PREFIX + asset3.getCabinetUnitPosition()+FlowgateConstant.SEPARATOR+"INLET",
            aggregatorService.getPositionInfo(asset3));

      Asset asset4 = new Asset();
      HashMap<String,String> justfication = new HashMap<String, String>();
      Map<String,String> sensorInfo1 = new HashMap<String,String>();
      sensorInfo1.put(FlowgateConstant.POSITION, "INLET");

      try {
         justfication.put(FlowgateConstant.SENSOR, mapper.writeValueAsString(sensorInfo1));
         asset4.setJustificationfields(justfication);
      } catch (JsonProcessingException e) {
         TestCase.fail();
      }

      TestCase.assertEquals("INLET",aggregatorService.getPositionInfo(asset4));
   }
}
