/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.jobtest;

import static org.mockito.Matchers.anyLong;

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

import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ValueUnit.MetricUnit;
import com.vmware.flowgate.poweriqworker.client.PowerIQAPIClient;
import com.vmware.flowgate.poweriqworker.jobs.PowerIQService;
import com.vmware.flowgate.poweriqworker.model.Inlet;
import com.vmware.flowgate.poweriqworker.model.InletReading;
import com.vmware.flowgate.poweriqworker.model.Outlet;
import com.vmware.flowgate.poweriqworker.model.OutletReading;
import com.vmware.flowgate.poweriqworker.model.Pdu;

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
      Mockito.when(this.powerIQAPIClient.getInlets(anyLong())).thenReturn(new ArrayList<Inlet>());
      Mockito.when(this.powerIQAPIClient.getOutlets(anyLong())).thenReturn(new ArrayList<Outlet>());
      Map<String,String> pduInfoMap = new HashMap<String,String>();
      pduInfoMap.put(FlowgateConstant.PDU_ID_FROM_POWERIQ, "123");
      List<ValueUnit> valueUnits = powerIQService.getValueUnits(pduInfoMap, powerIQAPIClient, null);
      TestCase.assertEquals(true, valueUnits.isEmpty());
   }

   @Test
   public void testGetValueUnits3() {
      Mockito.when(this.powerIQAPIClient.getInlets(128L)).thenReturn(getInlets());
      Mockito.when(this.powerIQAPIClient.getOutlets(128L)).thenReturn(getOutlets());
      Map<String,String> pduInfoMap = new HashMap<String,String>();
      pduInfoMap.put(FlowgateConstant.PDU_ID_FROM_POWERIQ, "128");
      List<ValueUnit> valueUnits = powerIQService.getValueUnits(pduInfoMap, powerIQAPIClient, createAdvanceSettingMap());
      TestCase.assertEquals(12, valueUnits.size());
      for(ValueUnit valueunit : valueUnits) {
         String extraidentifier = valueunit.getExtraidentifier();
         if(extraidentifier != null) {
            if(extraidentifier.equals(FlowgateConstant.INLET_NAME_PREFIX + 1)) {
               switch (valueunit.getKey()) {
               case MetricName.PDU_ACTIVE_POWER:
                  TestCase.assertEquals(getInlets().get(0).getReading().getActivePower()/1000, valueunit.getValueNum());
                  break;
               case MetricName.PDU_VOLTAGE:
                  TestCase.assertEquals(getInlets().get(0).getReading().getVoltage(), valueunit.getValueNum());
                  break;
               case MetricName.PDU_CURRENT:
                  TestCase.assertEquals(getInlets().get(0).getReading().getCurrent(), valueunit.getValueNum());
                  break;
               default:
                  break;
               }
            }else if(valueunit.getExtraidentifier().equals(FlowgateConstant.OUTLET_NAME_PREFIX + 1)) {
               switch (valueunit.getKey()) {
               case MetricName.PDU_ACTIVE_POWER:
                  TestCase.assertEquals(getOutlets().get(0).getReading().getActivePower()/1000, valueunit.getValueNum());
                  break;
               case MetricName.PDU_VOLTAGE:
                  TestCase.assertEquals(getOutlets().get(0).getReading().getVoltage(), valueunit.getValueNum());
                  break;
               case MetricName.PDU_CURRENT:
                  TestCase.assertEquals(getOutlets().get(0).getReading().getCurrent(), valueunit.getValueNum());
                  break;
               default:
                  break;
               }
            }else {
               TestCase.fail();
            }
         }else {
            switch (valueunit.getKey()) {
            case MetricName.PDU_TOTAL_POWER:
               TestCase.assertEquals(getInlets().get(0).getReading().getApparentPower()/1000, valueunit.getValueNum());
               break;
            case MetricName.PDU_TOTAL_CURRENT:
               TestCase.assertEquals(getInlets().get(0).getReading().getCurrent(), valueunit.getValueNum());
               break;
            default:
               TestCase.fail();
               break;
            }
         }

      }
   }

   @Test
   public void testGetRealTimeDatas() {
      List<RealTimeData> realTimeDatas = powerIQService.getRealTimeDatas(null,powerIQAPIClient,null);
      TestCase.assertEquals(true, realTimeDatas.isEmpty());
   }

   @Test
   public void testGetRealTimeDatas1() {
      Map<String,Map<String,String>> map = new HashMap<String,Map<String,String>>();
      List<RealTimeData> realTimeDatas = powerIQService.getRealTimeDatas(map,powerIQAPIClient,null);
      TestCase.assertEquals(true, realTimeDatas.isEmpty());
   }

   @Test
   public void testGetRealTimeDatas2() {
      Mockito.when(this.powerIQAPIClient.getInlets(128L)).thenReturn(getInlets());
      Mockito.when(this.powerIQAPIClient.getOutlets(128L)).thenReturn(getOutlets());
      Map<String,String> pduInfoMap = new HashMap<String,String>();
      pduInfoMap.put(FlowgateConstant.PDU_ID_FROM_POWERIQ, "128");
      Map<String,Map<String,String>> map = new HashMap<String,Map<String,String>>();
      map.put("123", pduInfoMap);
      HashMap<AdvanceSettingType,String> advanceSetting = createAdvanceSettingMap();
      advanceSetting.put(AdvanceSettingType.PDU_POWER_UNIT, MetricUnit.KW.toString());
      advanceSetting.put(AdvanceSettingType.PDU_VOLT_UNIT, MetricUnit.V.toString());
      advanceSetting.put(AdvanceSettingType.PDU_AMPS_UNIT, MetricUnit.A.toString());
      List<RealTimeData> realTimeDatas = powerIQService.getRealTimeDatas(map, powerIQAPIClient, advanceSetting);
      TestCase.assertEquals("123", realTimeDatas.get(0).getAssetID());
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

   List<Outlet> getOutlets() {
      Outlet outlet = new Outlet();
      List<Outlet> outlets = new ArrayList<Outlet>();
      outlet.setId(12L);
      outlet.setName("Outlet1");
      outlet.setOrdinal(1L);
      outlet.setPduId(128L);
      outlet.setRatedAmps(10.2);
      OutletReading reading = new OutletReading();
      reading.setCurrent(24.2);
      reading.setApparentPower(29.0);
      reading.setVoltage(200.0);
      reading.setReadingTime("2018/10/18 05:57:26 +0300");
      reading.setActivePower(27.6);
      reading.setUnutilizedCapacity(15.2);
      outlet.setReading(reading);
      outlets.add(outlet);
      return outlets;
   }

   List<Inlet> getInlets() {
      Inlet inlet = new Inlet();
      List<Inlet> inlets = new ArrayList<Inlet>();
      inlet.setId(23L);
      inlet.setOrdinal(1);
      inlet.setPduId(128L);
      inlet.setPueIt(true);
      inlet.setPueTotal(true);
      inlet.setSource(true);
      InletReading inletReading = new InletReading();
      inletReading.setCurrent(1.2);
      inletReading.setApparentPower(20.0);
      inletReading.setVoltage(200.0);
      inletReading.setReadingTime("2018/10/18 05:57:26 +0300");
      inletReading.setActivePower(26.6);
      inletReading.setUnutilizedCapacity(15.2);
      inlet.setReading(inletReading);
      inlets.add(inlet);
      return inlets;
   }
}
