/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.aggregator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vmware.flowgate.aggregator.redis.TestRedisConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.quartz.JobExecutionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.aggregator.scheduler.job.AggregatorService;
import com.vmware.flowgate.aggregator.scheduler.job.CustomerAdapterJobDispatcher;
import com.vmware.flowgate.aggregator.scheduler.job.OpenmanageJobDispatcher;
import com.vmware.flowgate.aggregator.scheduler.job.VCenterJobDispatcher;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.AdapterJobCommand;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilityAdapter;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.IntegrationStatus.Status;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestRedisConfiguration.class)
@ActiveProfiles("test")
public class MessageProcessingTest {

   @MockBean
   private StringRedisTemplate template;

   @Spy
   @InjectMocks
   private AggregatorService aggregatorService = new AggregatorService();

   @SpyBean
   private CustomerAdapterJobDispatcher customerAdapter;

   @SpyBean
   private VCenterJobDispatcher vcenterJobDispatcher;

   @MockBean
   private WormholeAPIClient restClient;

   @MockBean
   private ServiceKeyConfig serviceKeyConfig;

   @SpyBean
   private OpenmanageJobDispatcher openmanageJob;

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

      Asset asset5 = new Asset();
      asset5.setCabinetUnitPosition(3);
      HashMap<String,String> justfication5 = new HashMap<String, String>();
      Map<String,String> sensorInfo5 = new HashMap<String,String>();
      try {
         justfication5.put(FlowgateConstant.SENSOR, mapper.writeValueAsString(sensorInfo5));
         asset5.setJustificationfields(justfication5);
      } catch (JsonProcessingException e) {
         TestCase.fail();
      }

      TestCase.assertEquals(FlowgateConstant.RACK_UNIT_PREFIX + asset5.getCabinetUnitPosition(),aggregatorService.getPositionInfo(asset5));

      Asset asset6 = new Asset();
      HashMap<String,String> justfication6 = new HashMap<String, String>();
      Map<String,String> sensorInfo6 = new HashMap<String,String>();
      try {
         justfication6.put(FlowgateConstant.SENSOR, mapper.writeValueAsString(sensorInfo6));
         asset5.setJustificationfields(justfication6);
      } catch (JsonProcessingException e) {
         TestCase.fail();
      }

      TestCase.assertEquals(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION,aggregatorService.getPositionInfo(asset6));
   }

   @Test
   public void testRemovePduFromServer() {
      Asset[] servers = new Asset[2];
      Asset server = new Asset();
      List<String> pduids = new ArrayList<String>();
      pduids.add("qwiounasdyi2avewrasdf");
      pduids.add("mienoas2389asddsfqzda");
      server.setPdus(pduids);
      HashMap<String,String> justficationfields = new HashMap<String,String>();
      String pduPorts = "01"+FlowgateConstant.SEPARATOR+"pdu1"+FlowgateConstant.SEPARATOR+"01"+FlowgateConstant.SEPARATOR+"qwepouasdnlksaydasmnd"+FlowgateConstant.SPILIT_FLAG
            + "02"+FlowgateConstant.SEPARATOR+"pdu2"+FlowgateConstant.SEPARATOR+"02"+FlowgateConstant.SEPARATOR+"mienoas2389asddsfqzda";
      justficationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, pduPorts);
      server.setJustificationfields(justficationfields);
      Map<String, String> metricsFormular = new HashMap<String, String>();
      Map<String,Map<String,String>> pduMetricsFormula = new HashMap<String,Map<String,String>>();
      Map<String,String> pduinfo1 = new HashMap<String,String>();
      pduinfo1.put(MetricName.PDU_ACTIVE_POWER, "mienoas2389asddsfqzda");
      pduMetricsFormula.put("mienoas2389asddsfqzda", pduinfo1);

      Map<String,String> pduinfo2 = new HashMap<String,String>();
      pduinfo2.put(MetricName.PDU_ACTIVE_POWER, "asdasdw2213dsdfaewwqe");
      pduMetricsFormula.put("asdasdw2213dsdfaewwqe", pduinfo2);
      ObjectMapper mapper = new ObjectMapper();
      String pduMetricFormulasInfo = null;
      try {
         pduMetricFormulasInfo = mapper.writeValueAsString(pduMetricsFormula);
      } catch (JsonProcessingException e) {
        TestCase.fail(e.getMessage());
      }
      metricsFormular.put(FlowgateConstant.PDU, pduMetricFormulasInfo);
      server.setMetricsformulars(metricsFormular);
      servers[0] = server;

      Asset server1 = new Asset();
      List<String> pduids1 = new ArrayList<String>();
      pduids1.add("asdasdasd");
      pduids1.add("qweertwtc");
      server1.setPdus(pduids1);
      servers[1] = server1;

      HashSet<String> pduAssetIDs = new HashSet<String>();
      pduAssetIDs.add("mienoas2389asddsfqzda");
      pduAssetIDs.add("asdw2cvxcjchftyhretyv");
      List<Asset> needToUpdateServer = aggregatorService.removePduFromServer(servers, pduAssetIDs);
      TestCase.assertEquals(1, needToUpdateServer.size());
      Asset needupdateServer = needToUpdateServer.get(0);
      TestCase.assertEquals("01"+FlowgateConstant.SEPARATOR+"pdu1"+FlowgateConstant.SEPARATOR+"01"+FlowgateConstant.SEPARATOR+"qwepouasdnlksaydasmnd",
            needupdateServer.getJustificationfields().get(FlowgateConstant.PDU_PORT_FOR_SERVER));
      TestCase.assertEquals(1,needupdateServer.getPdus().size());
      TestCase.assertEquals("qwiounasdyi2avewrasdf", needupdateServer.getPdus().get(0));
      Map<String, String> metricsFormulars = needupdateServer.getMetricsformulars();
      TestCase.assertEquals(1, metricsFormulars.size());
      Map<String, Map<String, String>> pduMetricsFormulaMap = null;
      try {
         pduMetricsFormulaMap = mapper.readValue(metricsFormulars.get(FlowgateConstant.PDU), new TypeReference<Map<String, Map<String, String>>>() {});
      } catch (IOException e) {
         TestCase.fail(e.getMessage());
      }
      TestCase.assertEquals("asdasdw2213dsdfaewwqe", pduMetricsFormulaMap.keySet().iterator().next());
   }

   @Test
   public void testExecuteCustomerSendMessageJob() throws JobExecutionException {
      ListOperations<String, String> listOp = Mockito.mock(ListOperations.class);
      ValueOperations<String, String> valueOp = Mockito.mock(ValueOperations.class);
      when(template.opsForList()).thenReturn(listOp);
      when(listOp.leftPushAll(anyString(), anyString())).thenReturn(1L);
      when(template.hasKey(anyString())).thenReturn(true);
      when(template.opsForValue()).thenReturn(valueOp);

      FacilitySoftwareConfig fac1 = createFacilitySoftware();
      String unique_value1 = UUID.randomUUID().toString();
      fac1.setSubCategory("OtherDCIM_"+unique_value1);
      when(restClient.getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.OtherDCIM)).thenReturn(getFacilitySoftwareByType(fac1));

      FacilitySoftwareConfig fac2 = createFacilitySoftware();
      fac2.setType(FacilitySoftwareConfig.SoftwareType.OtherCMDB);
      String unique_value2 = UUID.randomUUID().toString();
      fac2.setSubCategory("OtherCMDB_"+unique_value2);

      when(restClient.getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.OtherCMDB)).thenReturn(getFacilitySoftwareByType(fac2));

      FacilityAdapter adapter = new FacilityAdapter();
      adapter.setSubCategory("OtherDCIM_"+unique_value1);
      AdapterJobCommand command1 = new AdapterJobCommand();
      command1.setCommand("syncmetadata");
      command1.setTriggerCycle(20);
      List<AdapterJobCommand> commands = new ArrayList<AdapterJobCommand>();
      commands.add(command1);
      adapter.setCommands(commands);
      adapter.setTopic(unique_value1);
      adapter.setQueueName(unique_value1+":joblist");

      FacilityAdapter adapter2 = new FacilityAdapter();
      adapter2.setSubCategory("OtherCMDB_"+unique_value2);
      AdapterJobCommand command2 = new AdapterJobCommand();
      command2.setCommand("syncmetadata");
      command2.setTriggerCycle(20);
      List<AdapterJobCommand> commands2 = new ArrayList<AdapterJobCommand>();
      commands2.add(command2);
      adapter2.setCommands(commands2);
      adapter2.setTopic(unique_value2);
      adapter2.setQueueName(unique_value2+":joblist");

      FacilityAdapter[] adapters = new FacilityAdapter[2];
      adapters[0] = adapter;
      adapters[1] = adapter2;
      when(restClient.getAllCustomerFacilityAdapters()).thenReturn(new ResponseEntity<FacilityAdapter[]>(adapters, HttpStatus.OK));
      customerAdapter.execute(null);
   }

   @Test
   public void testExecuteCustomerSendMessageJob1() throws JobExecutionException {
      ListOperations<String, String> listOp = Mockito.mock(ListOperations.class);
      ValueOperations<String, String> valueOp = Mockito.mock(ValueOperations.class);
      when(template.opsForList()).thenReturn(listOp);
      when(listOp.leftPushAll(anyString(), anyString())).thenReturn(1L);
      when(template.hasKey(anyString())).thenReturn(false);
      when(template.opsForValue()).thenReturn(valueOp);
      when(valueOp.get(anyString())).thenReturn("19");
      FacilitySoftwareConfig fac1 = createFacilitySoftware();
      String unique_value1 = UUID.randomUUID().toString();
      fac1.setSubCategory("OtherDCIM_"+unique_value1);
      when(restClient.getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.OtherDCIM)).thenReturn(getFacilitySoftwareByType(fac1));

      FacilitySoftwareConfig fac2 = createFacilitySoftware();
      fac2.setType(FacilitySoftwareConfig.SoftwareType.OtherCMDB);
      String unique_value2 = UUID.randomUUID().toString();
      fac2.setSubCategory("OtherCMDB_"+unique_value2);

      when(restClient.getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.OtherCMDB)).thenReturn(getFacilitySoftwareByType(fac2));

      FacilityAdapter adapter = new FacilityAdapter();
      adapter.setSubCategory("OtherDCIM_"+unique_value1);
      AdapterJobCommand command1 = new AdapterJobCommand();
      command1.setCommand("syncmetadata");
      command1.setTriggerCycle(20);
      List<AdapterJobCommand> commands = new ArrayList<AdapterJobCommand>();
      commands.add(command1);
      adapter.setCommands(commands);
      adapter.setTopic(unique_value1);
      adapter.setQueueName(unique_value1+":joblist");

      FacilityAdapter adapter2 = new FacilityAdapter();
      adapter2.setSubCategory("OtherCMDB_"+unique_value2);
      AdapterJobCommand command2 = new AdapterJobCommand();
      command2.setCommand("syncmetadata");
      command2.setTriggerCycle(20);
      List<AdapterJobCommand> commands2 = new ArrayList<AdapterJobCommand>();
      commands2.add(command2);
      adapter2.setCommands(commands2);
      adapter2.setTopic(unique_value2);
      adapter2.setQueueName(unique_value2+":joblist");

      FacilityAdapter[] adapters = new FacilityAdapter[2];
      adapters[0] = adapter;
      adapters[1] = adapter2;
      when(restClient.getAllCustomerFacilityAdapters()).thenReturn(new ResponseEntity<FacilityAdapter[]>(adapters, HttpStatus.OK));
      customerAdapter.execute(null);
   }

   @Test
   public void testExecuteCustomerSendMessageJob2() throws JobExecutionException {
      FacilitySoftwareConfig fac1 = createFacilitySoftware();
      String unique_value1 = UUID.randomUUID().toString();
      fac1.setSubCategory("OtherDCIM_"+unique_value1);
      when(restClient.getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.OtherDCIM)).thenReturn(getFacilitySoftwareByType(fac1));
      when(restClient.getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.OtherCMDB)).thenReturn(new ResponseEntity<FacilitySoftwareConfig[]>(new FacilitySoftwareConfig[0], HttpStatus.OK));

      FacilityAdapter adapter = new FacilityAdapter();
      adapter.setSubCategory("OtherDCIM_"+unique_value1);
      AdapterJobCommand command1 = new AdapterJobCommand();
      command1.setCommand("syncmetadata");
      command1.setTriggerCycle(20);
      List<AdapterJobCommand> commands = new ArrayList<AdapterJobCommand>();
      commands.add(command1);
      adapter.setCommands(commands);
      adapter.setTopic(unique_value1);
      adapter.setQueueName(unique_value1+":joblist");
      FacilityAdapter[] adapters = new FacilityAdapter[1];
      adapters[0] = adapter;
      when(restClient.getAllCustomerFacilityAdapters()).thenReturn(new ResponseEntity<FacilityAdapter[]>(new FacilityAdapter[0], HttpStatus.OK));
      customerAdapter.execute(null);
   }

   @Test
   public void testExecuteCustomerSendMessageJob3() throws JobExecutionException {
      ListOperations<String, String> listOp = Mockito.mock(ListOperations.class);
      ValueOperations<String, String> valueOp = Mockito.mock(ValueOperations.class);
      when(template.opsForList()).thenReturn(listOp);
      when(listOp.leftPushAll(anyString(), anyString())).thenReturn(1L);
      when(template.hasKey(anyString())).thenReturn(true);
      when(template.opsForValue()).thenReturn(valueOp);
      when(valueOp.increment(anyString())).thenReturn(4l);

      FacilitySoftwareConfig fac1 = createFacilitySoftware();
      String unique_value1 = UUID.randomUUID().toString();
      fac1.setSubCategory("OtherDCIM_"+unique_value1);
      when(restClient.getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.OtherDCIM)).thenReturn(new ResponseEntity<FacilitySoftwareConfig[]>(new FacilitySoftwareConfig[0], HttpStatus.OK));
      when(restClient.getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.OtherCMDB)).thenReturn(new ResponseEntity<FacilitySoftwareConfig[]>(new FacilitySoftwareConfig[0], HttpStatus.OK));

      FacilityAdapter adapter = new FacilityAdapter();
      adapter.setSubCategory("OtherDCIM_"+unique_value1);
      AdapterJobCommand command1 = new AdapterJobCommand();
      command1.setCommand("syncmetadata");
      command1.setTriggerCycle(20);
      List<AdapterJobCommand> commands = new ArrayList<AdapterJobCommand>();
      commands.add(command1);
      adapter.setCommands(commands);
      adapter.setTopic(unique_value1);
      adapter.setQueueName(unique_value1+":joblist");
      FacilityAdapter[] adapters = new FacilityAdapter[1];
      adapters[0] = adapter;
      when(restClient.getAllCustomerFacilityAdapters()).thenReturn(new ResponseEntity<FacilityAdapter[]>(adapters, HttpStatus.OK));
      customerAdapter.execute(null);
   }

   @Test
   public void testVcJobExecute() throws JobExecutionException, JsonProcessingException {

      ListOperations<String, String> listOp = Mockito.mock(ListOperations.class);
      ValueOperations<String, String> valueOp = Mockito.mock(ValueOperations.class);
      SDDCSoftwareConfig[] sDDCSoftwareConfigs = new SDDCSoftwareConfig[2];
      sDDCSoftwareConfigs[0] = Mockito.mock(SDDCSoftwareConfig.class);
      sDDCSoftwareConfigs[1] = Mockito.mock(SDDCSoftwareConfig.class);
      ResponseEntity<SDDCSoftwareConfig[]> resp = Mockito.mock(ResponseEntity.class);
      List<String> lists = new ArrayList<>();

      when(template.opsForValue()).thenReturn(valueOp);
      when(valueOp.get(anyString())).thenReturn("2880");
      when(serviceKeyConfig.getServiceKey()).thenReturn("");
      doNothing().when(valueOp).set(anyString(), anyString());

      when(restClient.getInternalSDDCSoftwareConfigByType(SoftwareType.VCENTER)).thenReturn(resp);
      when(resp.getBody()).thenReturn(sDDCSoftwareConfigs);

      when(sDDCSoftwareConfigs[0].checkIsActive()).thenReturn(true);
      when(sDDCSoftwareConfigs[1].checkIsActive()).thenReturn(true);

      when(template.opsForList()).thenReturn(listOp);

      doReturn(lists).when(vcenterJobDispatcher).generateSDDCMessageListByType("", sDDCSoftwareConfigs);

      when(listOp.leftPushAll("", lists)).thenReturn(1L);

      vcenterJobDispatcher.execute(null);
   }

   @Test
   public void testOpenManageJobExecute() throws JobExecutionException, JsonProcessingException {
      ListOperations<String, String> listOp = Mockito.mock(ListOperations.class);
      ValueOperations<String, String> valueOp = Mockito.mock(ValueOperations.class);
      FacilitySoftwareConfig[] integrations = new FacilitySoftwareConfig[2];
      integrations[0] = Mockito.mock(FacilitySoftwareConfig.class);
      integrations[1] = Mockito.mock(FacilitySoftwareConfig.class);
      ResponseEntity<FacilitySoftwareConfig[]> resp = Mockito.mock(ResponseEntity.class);
      List<String> lists = new ArrayList<>();

      when(template.opsForValue()).thenReturn(valueOp);
      when(valueOp.get(anyString())).thenReturn("2880");
      when(serviceKeyConfig.getServiceKey()).thenReturn("");
      doNothing().when(valueOp).set(anyString(), anyString());
      when(restClient.getFacilitySoftwareInternalByType(any(com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType.class))).thenReturn(resp);
      when(resp.getBody()).thenReturn(integrations);
      when(integrations[0].checkIsActive()).thenReturn(true);
      when(integrations[1].checkIsActive()).thenReturn(true);
      when(template.opsForList()).thenReturn(listOp);
      when(listOp.leftPushAll("", lists)).thenReturn(1L);

      openmanageJob.execute(null);
   }

   @Test
   public void testOpenManageJobExecute1() throws JobExecutionException, JsonProcessingException {
      ListOperations<String, String> listOp = Mockito.mock(ListOperations.class);
      ValueOperations<String, String> valueOp = Mockito.mock(ValueOperations.class);
      FacilitySoftwareConfig[] integrations = new FacilitySoftwareConfig[2];
      integrations[0] = Mockito.mock(FacilitySoftwareConfig.class);
      integrations[1] = Mockito.mock(FacilitySoftwareConfig.class);
      ResponseEntity<FacilitySoftwareConfig[]> resp = Mockito.mock(ResponseEntity.class);
      List<String> lists = new ArrayList<>();

      when(template.opsForValue()).thenReturn(valueOp);
      when(valueOp.get(anyString())).thenReturn(null);
      when(serviceKeyConfig.getServiceKey()).thenReturn("");
      doNothing().when(valueOp).set(anyString(), anyString());
      when(restClient.getFacilitySoftwareInternalByType(any(com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType.class))).thenReturn(resp);
      when(resp.getBody()).thenReturn(integrations);
      when(integrations[0].checkIsActive()).thenReturn(true);
      when(integrations[1].checkIsActive()).thenReturn(true);
      when(template.opsForList()).thenReturn(listOp);
      when(listOp.leftPushAll("", lists)).thenReturn(1L);

      openmanageJob.execute(null);
   }

   /**
    * SERVER_VOLTAGE = "Voltage";
     SERVER_CONNECTED_PDU_CURRENT = "Current";
     SERVER_CONNECTED_PDU_POWER = "Power";
     SERVER_CONNECTED_PDU_CURRENT_LOAD = "CurrentLoad";
     SERVER_CONNECTED_PDU_POWER_LOAD = "PowerLoad";
     SERVER_USED_PDU_OUTLET_CURRENT = "%s|Current";
     SERVER_USED_PDU_OUTLET_POWER = "%s|Power";
     SERVER_USED_PDU_OUTLET_VOLTAGE = "%s|Voltage";
    */
   @Test
   public void testGeneratePduformulaForServer() {
      List<String> pduIds = new ArrayList<String>();
      pduIds.add("12pimppoqwemasdqweggrwq");
      pduIds.add("123456");
      Map<String,Map<String,String>> pduFormula = aggregatorService.generatePduformulaForServer(pduIds);
      TestCase.assertEquals(pduIds.size(), pduFormula.size());
      TestCase.assertEquals(8, pduFormula.get(pduIds.get(0)).size());
   }

   FacilitySoftwareConfig createFacilitySoftware() {
      FacilitySoftwareConfig example = new FacilitySoftwareConfig();
      example.setId(UUID.randomUUID().toString());
      example.setName("OtherDcimSample");
      example.setUserName("administrator@vsphere.local");
      example.setPassword("Admin!23");
      example.setServerURL("https://10.160.30.134");
      example.setType(FacilitySoftwareConfig.SoftwareType.OtherDCIM);
      example.setUserId("1");
      example.setVerifyCert(false);
      example.setDescription("description");
      HashMap<AdvanceSettingType, String> advanceSetting = new HashMap<AdvanceSettingType, String>();
      example.setAdvanceSetting(advanceSetting);
      IntegrationStatus integrationStatus = new IntegrationStatus();
      integrationStatus.setDetail("");
      integrationStatus.setRetryCounter(0);
      integrationStatus.setStatus(Status.ACTIVE);
      example.setIntegrationStatus(integrationStatus);
      return example;
   }

   public ResponseEntity<FacilitySoftwareConfig[]> getFacilitySoftwareByType(FacilitySoftwareConfig intergration) {
      FacilitySoftwareConfig[] configs = new FacilitySoftwareConfig[1];
      configs[0] = intergration;
      return new ResponseEntity<FacilitySoftwareConfig[]>(configs, HttpStatus.OK);
   }
}
