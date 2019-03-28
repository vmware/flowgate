/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.jobtest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.flowgate.poweriqworker.client.PowerIQAPIClient;
import com.vmware.flowgate.poweriqworker.jobs.PowerIQService;
import com.vmware.flowgate.poweriqworker.model.InletReading;
import com.vmware.flowgate.poweriqworker.model.Pdu;
import com.vmware.flowgate.poweriqworker.model.Reading;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ValueUnit.MetricUnit;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SyncRealTimeDataJobTest {

   @Mock
   private WormholeAPIClient wormholeAPIClient;

   @Mock
   private PowerIQAPIClient powerIQAPIClient;

   @Spy
   @InjectMocks
   private PowerIQService powerIQService;

   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
      Mockito.when(this.wormholeAPIClient.getFacilitySoftwareByType(SoftwareType.PowerIQ))
            .thenReturn(getFacilitySoftwareByType());
      Mockito.when(this.powerIQAPIClient.getPdus()).thenReturn(getPdus());
   }

   @Test
   public void testGetValueUnits() {
      Pdu pdu = null;
      List<ValueUnit> valueUnits = powerIQService.getValueUnits(pdu,null);
      TestCase.assertEquals(true, valueUnits.isEmpty());
   }

   @Test
   public void testGetValueUnits1() {
      Pdu pdu = createPdu();
      List<ValueUnit> valueUnits = powerIQService.getValueUnits(pdu,null);
      TestCase.assertEquals(true, valueUnits.isEmpty());
   }

   @Test
   public void testGetValueUnits2() {
      Pdu pdu = createPdu();
      Reading reading = new Reading();
      pdu.setReading(reading);
      List<ValueUnit> valueUnits = powerIQService.getValueUnits(pdu,null);
      TestCase.assertEquals(true, valueUnits.isEmpty());

   }

   @Test
   public void testGetValueUnits3() {
      Pdu pdu = createPdu();
      Reading reading = new Reading();
      pdu.setReading(reading);
      List<InletReading> inletReadings = new ArrayList<InletReading>();
      reading.setInletReadings(inletReadings);
      List<ValueUnit> valueUnits = powerIQService.getValueUnits(pdu,createAdvanceSettingMap());
      TestCase.assertEquals(0, valueUnits.size());

   }

   @Test
   public void testGetValueUnits4() {
      Pdu pdu = createPdu();
      Reading reading = new Reading();
      List<InletReading> inletReadings = new ArrayList<InletReading>();
      InletReading inletReading1 = createInletReading();
      InletReading inletReading2 = createInletReading();
      inletReadings.add(inletReading1);
      inletReadings.add(inletReading2);
      reading.setInletReadings(inletReadings);
      pdu.setReading(reading);
      HashMap<AdvanceSettingType,String> advanceSetting = createAdvanceSettingMap();
      advanceSetting.put(AdvanceSettingType.PDU_POWER_UNIT, MetricUnit.W.toString());
      advanceSetting.put(AdvanceSettingType.PDU_VOLT_UNIT, MetricUnit.KV.toString());
      advanceSetting.put(AdvanceSettingType.PDU_AMPS_UNIT, MetricUnit.A.toString());
      List<ValueUnit> valueUnits = powerIQService.getValueUnits(pdu, advanceSetting);
      for (ValueUnit valueUnit : valueUnits) {
         switch (valueUnit.getKey()) {
         case PDU_RealtimeVoltage:
            TestCase.assertEquals(200000.0, valueUnit.getValueNum());
            continue;
         case PDU_RealtimeLoad:
            TestCase.assertEquals(2.4, valueUnit.getValueNum());
            continue;
         case PDU_RealtimePower:
            TestCase.assertEquals(0.04, valueUnit.getValueNum());
            continue;
         default:
            break;
         }
      }
   }

   @Test
   public void testGetRealTimeDatas() {
      List<RealTimeData> realTimeDatas = powerIQService.getRealTimeDatas(null,null);
      TestCase.assertEquals(true, realTimeDatas.isEmpty());
   }

   @Test
   public void testGetRealTimeDatas1() {
      Map<String, Pdu> pdus = new HashMap<String, Pdu>();
      List<RealTimeData> realTimeDatas = powerIQService.getRealTimeDatas(pdus,null);
      TestCase.assertEquals(true, realTimeDatas.isEmpty());
   }

   @Test
   public void testGetRealTimeDatas2() {
      Map<String, Pdu> pdusMap = new HashMap<String, Pdu>();
      Pdu pdu = createPdu();
      Reading reading = new Reading();
      List<InletReading> inletReadings = new ArrayList<InletReading>();
      InletReading inletReading1 = createInletReading();
      InletReading inletReading2 = createInletReading();
      inletReadings.add(inletReading1);
      inletReadings.add(inletReading2);
      reading.setInletReadings(inletReadings);
      pdu.setReading(reading);
      pdusMap.put("123", pdu);
      HashMap<AdvanceSettingType,String> advanceSetting = createAdvanceSettingMap();
      advanceSetting.put(AdvanceSettingType.PDU_POWER_UNIT, MetricUnit.KW.toString());
      advanceSetting.put(AdvanceSettingType.PDU_VOLT_UNIT, MetricUnit.KV.toString());
      advanceSetting.put(AdvanceSettingType.PDU_AMPS_UNIT, MetricUnit.A.toString());
      List<RealTimeData> realTimeDatas = powerIQService.getRealTimeDatas(pdusMap, advanceSetting);
      TestCase.assertEquals("123", realTimeDatas.get(0).getAssetID());
   }
   
   @Test
   public void testGetRealTimeDatas3() {
      Map<String, Pdu> pdusMap = new HashMap<String, Pdu>();
      HashMap<AdvanceSettingType,String> advanceSetting = new HashMap<AdvanceSettingType, String>();
      advanceSetting.put(AdvanceSettingType.PDU_POWER_UNIT, MetricUnit.KW.toString());
      advanceSetting.put(AdvanceSettingType.PDU_VOLT_UNIT, MetricUnit.KV.toString());
      advanceSetting.put(AdvanceSettingType.PDU_AMPS_UNIT, MetricUnit.A.toString());
      advanceSetting.put(AdvanceSettingType.DateFormat, "yyyy/MM/dd HH:mm:ss Z");
      Pdu pdu = createPdu();
      Reading reading = new Reading();
      List<InletReading> inletReadings = new ArrayList<InletReading>();
      InletReading inletReading1 = createInletReading();
      InletReading inletReading2 = createInletReading();
      inletReadings.add(inletReading1);
      inletReadings.add(inletReading2);
      reading.setInletReadings(inletReadings);
      pdu.setReading(reading);
      pdusMap.put("123", pdu);
      List<RealTimeData> realTimeDatas = powerIQService.getRealTimeDatas(pdusMap, advanceSetting);
      TestCase.assertEquals(40.0, realTimeDatas.get(0).getValues().get(2).getValueNum());
      TestCase.assertEquals(2.4, realTimeDatas.get(0).getValues().get(1).getValueNum());
      TestCase.assertEquals(200000.0, realTimeDatas.get(0).getValues().get(0).getValueNum());
   }

   @Test
   public void testGetMatchedPdus() {
      Map<String, Pdu> map = powerIQService.getMatchedPdus(null, null);
      TestCase.assertEquals(true, map.isEmpty());
      Map<String, Pdu> filterCondition = new HashMap<String, Pdu>();
      Map<String, Pdu> map1 = powerIQService.getMatchedPdus(filterCondition, null);
      TestCase.assertEquals(true, map1.isEmpty());
   }

   @Test
   public void testGetMatchedPdus1() {
      Pdu pdu = createPdu();
      pdu.setName("pek-wor-pdu");
      Asset asset = createAsset();
      List<Asset> assets = new ArrayList<Asset>();
      assets.add(asset);
      Map<String, Pdu> filterCondition = new HashMap<String, Pdu>();
      filterCondition.put(pdu.getName(), pdu);
      Map<String, Pdu> map = powerIQService.getMatchedPdus(filterCondition, assets);
      TestCase.assertEquals(true, map.isEmpty());
   }

   @Test
   public void testGetMatchedPdus2() {
      Pdu pdu = createPdu();
      pdu.setName("pek-wor-pdu-02");
      Asset asset = createAsset();
      asset.setId("abc");
      List<Asset> assets = new ArrayList<Asset>();
      assets.add(asset);
      Map<String, Pdu> filterCondition = new HashMap<String, Pdu>();
      filterCondition.put(pdu.getName(), pdu);
      Map<String, Pdu> pdusmap = powerIQService.getMatchedPdus(filterCondition, assets);
      for (Map.Entry<String, Pdu> map : pdusmap.entrySet()) {
         if ("pek-wor-pdu-02".equals(map.getValue().getName())) {
            TestCase.assertEquals("abc", map.getKey());
         }
      }
   }

   @Test
   public void testGetPdusMap() {
      List<Pdu> pdus =  getPdus();
      Map<String, Pdu> pdusmap = powerIQService.getPdusMapWithNameKey(pdus);
      TestCase.assertEquals(true, pdusmap.containsKey("pdu-1"));
      TestCase.assertEquals(true, pdusmap.containsKey("pek-wor-pdu-02"));
   }

   public ResponseEntity<FacilitySoftwareConfig[]> getFacilitySoftwareByType() {
      FacilitySoftwareConfig[] configs = new FacilitySoftwareConfig[1];
      configs[0] = new FacilitySoftwareConfig();
      configs[0].setId("l9i8728d55368540fcba1692");
      configs[0].setType(SoftwareType.PowerIQ);
      return new ResponseEntity<FacilitySoftwareConfig[]>(configs, HttpStatus.OK);
   }

   ValueUnit createValueUnit() {
      ValueUnit value = new ValueUnit();
      return value;
   }

   HashMap<AdvanceSettingType, String> createAdvanceSettingMap(){
      HashMap<AdvanceSettingType, String> advanceSettingMap = new HashMap<AdvanceSettingType, String>();
      advanceSettingMap.put(AdvanceSettingType.DateFormat, "yyyy/MM/dd HH:mm:ss Z");
      return advanceSettingMap;
   }
   
   Asset createAsset() {
      Asset asset = new Asset();
      asset.setAssetName("pek-wor-pdu-02");
      asset.setAssetNumber(12345);
      asset.setAssetSource("l9i8728d55368540fcba1692");
      asset.setCategory(AssetCategory.PDU);
      asset.setModel("Dell 750");
      asset.setManufacturer("Dell");
      return asset;
   }

   Pdu createPdu() {
      Pdu pdu = new Pdu();
      pdu.setName("pek-wor-pdu-02");
      return pdu;
   }

   List<Pdu> getPdus() {
      List<Pdu> pdus = new ArrayList<Pdu>();
      Pdu pdu1 = createPdu();
      pdu1.setId(1);
      pdu1.setName("pdu-1");
      Pdu pdu2 = createPdu();
      pdu2.setId(2);
      pdus.add(pdu1);
      pdus.add(pdu2);
      return pdus;
   }

   InletReading createInletReading() {
      InletReading inletReading = new InletReading();
      inletReading.setCurrent(1.2);
      inletReading.setApparentPower(20.0);
      inletReading.setVoltage(200.0);
      inletReading.setReadingTime("2018/10/18 05:57:26 +0300");
      return inletReading;
   }
}
