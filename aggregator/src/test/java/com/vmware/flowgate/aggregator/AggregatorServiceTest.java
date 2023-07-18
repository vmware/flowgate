/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.flowgate.aggregator;

import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.aggregator.redis.TestRedisConfiguration;
import com.vmware.flowgate.aggregator.scheduler.job.AggregatorService;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.IntegrationStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestRedisConfiguration.class)
@ActiveProfiles("test")
public class AggregatorServiceTest {

   @Spy
   @InjectMocks
   private AggregatorService aggregatorService;
   @Mock
   private WormholeAPIClient restClient;
   @Mock
   private ServiceKeyConfig serviceKeyConfig;

   @Before
   public void before() {
      Mockito.doReturn("FLOWGATETEST").when(serviceKeyConfig).getServiceKey();
      Mockito.doNothing().when(restClient).setServiceKey(Mockito.anyString());
   }

   @Test
   public void testAggregateAndCleanPDUFromPowerIQ() {
      Mockito.doReturn(getFacilitySoftware()).when(restClient).getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.PowerIQ);
      Mockito.doReturn(getPdus()).when(restClient).getAllAssetsByType(AssetCategory.PDU);
      Mockito.doReturn(new ResponseEntity<Void>(HttpStatus.OK)).when(restClient).saveAssets(Mockito.any(Asset.class));
      Mockito.doReturn(new ResponseEntity<Void>(HttpStatus.OK)).when(restClient).removeAssetByID(Mockito.anyString());
      Mockito.doReturn(new ResponseEntity<Void>(HttpStatus.OK)).when(restClient).getServersWithPDUInfo();
      aggregatorService.aggregateAndCleanPDUFromPowerIQ();
   }

   private ResponseEntity<FacilitySoftwareConfig[]> getFacilitySoftware() {
      FacilitySoftwareConfig[] facilitySoftwareConfigs = new FacilitySoftwareConfig[1];
      FacilitySoftwareConfig facilitySoftwareConfig = new FacilitySoftwareConfig();
      facilitySoftwareConfig.setId("QWENBADJWRTUIJWJAYQY");
      facilitySoftwareConfig.setPassword("O75xginpkAD748w=Lc20CrTzd1lEpvDTdJqH5IXBZTb5gYp7P8awDAs19F0=");
      facilitySoftwareConfig.setServerURL("https://10.161.71.133");
      facilitySoftwareConfig.setName("powerIQ-1");
      facilitySoftwareConfig.setVerifyCert(false);
      IntegrationStatus integrationStatus = new IntegrationStatus();
      integrationStatus.setRetryCounter(0);
      integrationStatus.setDetail("");
      integrationStatus.setStatus(IntegrationStatus.Status.ACTIVE);
      facilitySoftwareConfig.setIntegrationStatus(integrationStatus);
      facilitySoftwareConfig.setUserName("admin");
      facilitySoftwareConfig.setType(FacilitySoftwareConfig.SoftwareType.PowerIQ);
      facilitySoftwareConfig.setUserId("e1edfv8953002379827896a1aaiqoose");
      facilitySoftwareConfigs[0] = facilitySoftwareConfig;
      return new ResponseEntity<>(facilitySoftwareConfigs, HttpStatus.OK);
   }

   private List<Asset> getPdus() {
      Asset asset1 = new Asset();
      asset1.setId("ASDFASDFASDFASDF");
      asset1.setAssetName("PDU-1");
      asset1.setAssetNumber(12345);
      asset1.setAssetSource("QWENBADJWRTUIJWJAYQY");
      asset1.setCategory(AssetCategory.PDU);
      setPowerIQPduFormulas(asset1);
      Asset asset2 = new Asset();
      asset2.setId("QOBNQHJGOQAJVJQO");
      asset2.setAssetName("PDU-2");
      asset2.setAssetNumber(54321);
      asset2.setAssetSource("QWENBADJWRTUIJWJAYQY");
      asset2.setCategory(AssetCategory.PDU);
      setPowerIQPduFormulas(asset2);

      Asset asset3 = new Asset();
      asset3.setId("ASDFASDFASDFASDF");
      asset3.setAssetName("PDU-1");
      asset3.setAssetNumber(12345);
      asset3.setAssetSource("GIBNQOPVBNJQJVPQGJQJK");
      asset3.setCategory(AssetCategory.PDU);
      setPduFormulas(asset3);
      Asset asset4 = new Asset();
      asset4.setId("QOBNQHJGOQAJVJQO");
      asset4.setAssetName("PDU-2");
      asset4.setAssetNumber(54321);
      asset4.setAssetSource("GIBNQOPVBNJQJVPQGJQJK");
      asset4.setCategory(AssetCategory.PDU);
      setPduFormulas(asset4);
      return new ArrayList<>(Arrays.asList(asset1, asset2, asset3, asset4));
   }

   private void setPowerIQPduFormulas(Asset pdu) {
      Map<String, String> pduFormulaMap = new HashMap<>();
      pduFormulaMap.put(MetricName.PDU_CURRENT, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_TOTAL_POWER, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_CURRENT_LOAD, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_POWER_LOAD, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_XLET_ACTIVE_POWER, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_XLET_APPARENT_POWER, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_XLET_FREE_CAPACITY, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_XLET_CURRENT, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_XLET_VOLTAGE, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_INLET_XPOLE_FREE_CAPACITY, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_INLET_XPOLE_CURRENT, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_INLET_XPOLE_VOLTAGE, pdu.getId());

      Map<String, Map<String, String>> sensorFormulaMap = new HashMap<>();
      Map<String, String> temperatureFormulaMap = new HashMap<>();
      temperatureFormulaMap.put("INLET", "3b1f65d2adfe43999c7fe3466b2b8021");
      sensorFormulaMap.put("Temperature", temperatureFormulaMap);
      Map<String, String> humidityFormulaMap = new HashMap<>();
      humidityFormulaMap.put("INLET", "ab2b5fb7cc184246a3525d9748775157");
      sensorFormulaMap.put("Humidity", humidityFormulaMap);

      Map<String, String> metricsFormulasMap = new HashMap<>();
      metricsFormulasMap.put(FlowgateConstant.PDU, pdu.metricsFormulaToString(pduFormulaMap));
      metricsFormulasMap.put(FlowgateConstant.SENSOR, pdu.metricsFormulaToString(sensorFormulaMap));
      pdu.setMetricsformulars(metricsFormulasMap);
   }

   private void setPduFormulas(Asset pdu) {
      Map<String, String> pduFormulaMap = new HashMap<>();
      pduFormulaMap.put(MetricName.PDU_CURRENT, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_TOTAL_POWER, pdu.getId());
      pduFormulaMap.put(MetricName.PDU_VOLTAGE, pdu.getId());
      Map<String, String> hashMap = new HashMap<>();
      hashMap.put(FlowgateConstant.PDU, pdu.metricsFormulaToString(pduFormulaMap));

      Map<String, Map<String, String>> sensorFormulaMap = new HashMap<>();
      Map<String, String> temperatureFormulaMap = new HashMap<>();
      temperatureFormulaMap.put("FRONT", "5dab088ab8a94aa994fdf9ddd8acceb6");
      sensorFormulaMap.put("Temperature", temperatureFormulaMap);
      Map<String, String> humidityFormulaMap = new HashMap<>();
      humidityFormulaMap.put("FRONT", "9442c4636c0a4fb7a2a6ac090227cc4f");
      sensorFormulaMap.put("Humidity", humidityFormulaMap);
      hashMap.put(FlowgateConstant.SENSOR, pdu.metricsFormulaToString(sensorFormulaMap));
      pdu.setMetricsformulars(hashMap);
   }

}
