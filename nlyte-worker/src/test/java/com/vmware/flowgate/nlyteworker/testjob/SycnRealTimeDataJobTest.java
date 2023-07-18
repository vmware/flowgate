/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.testjob;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vmware.flowgate.nlyteworker.redis.TestRedisConfiguration;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.MountingSide;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.FlowgateChassisSlot;
import com.vmware.flowgate.common.model.Parent;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.Tenant;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.nlyteworker.config.ServiceKeyConfig;
import com.vmware.flowgate.nlyteworker.model.ChassisMountedAssetMap;
import com.vmware.flowgate.nlyteworker.model.ChassisSlot;
import com.vmware.flowgate.nlyteworker.model.JsonResultForPDURealtimeValue;
import com.vmware.flowgate.nlyteworker.model.LocationGroup;
import com.vmware.flowgate.nlyteworker.model.Manufacturer;
import com.vmware.flowgate.nlyteworker.model.Material;
import com.vmware.flowgate.nlyteworker.model.NlyteAsset;
import com.vmware.flowgate.nlyteworker.model.PowerStripsRealtimeValue;
import com.vmware.flowgate.nlyteworker.restclient.NlyteAPIClient;
import com.vmware.flowgate.nlyteworker.scheduler.job.NlyteDataService;
import com.vmware.flowgate.nlyteworker.scheduler.job.common.HandleAssetUtil;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestRedisConfiguration.class)
@ActiveProfiles("test")
public class SycnRealTimeDataJobTest {

   @Mock
   private WormholeAPIClient wormholeAPIClient;

   @Mock
   private NlyteAPIClient nlyteAPIClient;

   @Mock
   private ServiceKeyConfig config;

   private ObjectMapper mapper = new ObjectMapper();

   @Spy
   @InjectMocks
   private NlyteDataService nlyteDataService = new NlyteDataService();

   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
      Mockito.when(this.wormholeAPIClient.getFacilitySoftwareInternalByType(SoftwareType.Nlyte)).thenReturn(getFacilitySoftwareByType());
      Mockito.when(this.wormholeAPIClient.getMappedAsset(AssetCategory.Server)).thenReturn(getMappedAsset());
      Mockito.when(this.wormholeAPIClient.getAssetByID("5x4ff46982db22e1b040e0f2")).thenReturn(getAssetByID());
      Mockito.when(this.wormholeAPIClient.saveRealTimeData(anyList())).thenReturn(saveRealTimeData());
      Mockito.when(this.nlyteAPIClient.getPowerStripsRealtimeValue(12345)).thenReturn(getPowerStripsRealtimeValue());
   }

   @Test
   public void testSycnRealTimeDataJob() {
      Mockito.doReturn(getServiceKey()).when(config).getServiceKey();
      Mockito.doReturn(nlyteAPIClient).when(nlyteDataService).createClient(any(FacilitySoftwareConfig.class));

      nlyteDataService.syncRealtimeData(getFacilitySoftwareByType().getBody().clone()[0]);
   }

   @Test
   public void testGenerateNewAsset() {
      List<NlyteAsset> nlyteAssets = getNlyteAsset();
      HashMap<Integer, LocationGroup> locationMap = getLocationMap();
      HashMap<Integer, Material> materialMap = new HashMap<Integer, Material>();
      Material material = new Material();
      material.setMaterialID(6251);
      material.setManufacturerID(14);
      material.setMaterialName("Cisco 1721 Modular Access Router");
      material.setMaterialType(AssetCategory.Server);
      material.setMaterialSubtype(AssetSubCategory.Blade);
      materialMap.put(6251, material);
      HashMap<Integer,Manufacturer> manufacturerMap = new HashMap<Integer,Manufacturer>();
      Manufacturer manufacturer = new Manufacturer();
      manufacturer.setManufacturerID(14);
      manufacturer.setDetail("Cisco");
      manufacturerMap.put(14, manufacturer);
      Mockito.when(this.wormholeAPIClient.getAllAssetsBySourceAndType("l9i8728d55368540fcba1692",AssetCategory.Server))
      .thenReturn(new ArrayList<Asset>());
      HashMap<Long,String> chassisMountedAssetNumberAndChassisIDMap = new HashMap<Long,String>();
      chassisMountedAssetNumberAndChassisIDMap.put(197L, "asdqe945kjsdf09uw45ms");
      List<Asset>assets = nlyteDataService.generateAssets("l9i8728d55368540fcba1692", nlyteAssets,
            locationMap, manufacturerMap, materialMap, AssetCategory.Server, chassisMountedAssetNumberAndChassisIDMap);

      for(Asset asset:assets) {
         if("sin2-blrqeops-esxstress024".equals(asset.getAssetName())) {
            TestCase.assertEquals(197, asset.getAssetNumber());
            TestCase.assertEquals("SG-07-04", asset.getRoom());
            TestCase.assertEquals("Cisco 1721 Modular Access Router", asset.getModel());
            TestCase.assertEquals("Cisco", asset.getManufacturer());
            TestCase.assertEquals("asdqe945kjsdf09uw45ms", asset.getParent().getParentId());
         }
      }
   }

   @Test
   public void testGenerateNewAsset1() {
      List<NlyteAsset> nlyteAssets = getNlyteAsset();
      HashMap<Integer, LocationGroup> locationMap = getLocationMap();
      HashMap<Integer, Material> materialMap = getMaterialMap();
      HashMap<Integer, Manufacturer> manufacturerMap = getManufacturerMap();
      List<Asset> assetsFromFlowgate = new ArrayList<Asset>();
      Asset preAsset = new Asset();
      preAsset.setAssetNumber(197);
      preAsset.setAssetSource("l9i8728d55368540fcba1692");
      assetsFromFlowgate.add(preAsset);
      Mockito.when(this.wormholeAPIClient.getAllAssetsBySourceAndType("l9i8728d55368540fcba1692", AssetCategory.Server))
      .thenReturn(assetsFromFlowgate);
      HashMap<Integer,String> cabinetIdAndNameMap = new HashMap<Integer, String>();
      cabinetIdAndNameMap.put(562, "cbName");
      nlyteAssets = nlyteDataService.supplementCabinetName(cabinetIdAndNameMap, nlyteAssets);
      List<Asset> assets = nlyteDataService.generateAssets("l9i8728d55368540fcba1692", nlyteAssets,
            locationMap, manufacturerMap, materialMap, AssetCategory.Server, new HashMap<Long,String>());
      for(Asset asset:assets) {
         if("sin2-blrqeops-esxstress024".equals(asset.getAssetName())) {
            TestCase.assertEquals(197, asset.getAssetNumber());
            TestCase.assertEquals("SG-07-04", asset.getRoom());
            TestCase.assertEquals("Cisco 1721 Modular Access Router", asset.getModel());
            TestCase.assertEquals("Cisco", asset.getManufacturer());
            TestCase.assertEquals("cbName", asset.getCabinetName());
         }
      }
   }

   @Test
   public void testGenerateNewAsset2() {
      List<NlyteAsset> nlyteAssets = getNlyteAsset();
      HashMap<Integer, LocationGroup> locationMap = getLocationMap();
      HashMap<Integer, Material> materialMap = new HashMap<Integer, Material>();
      Material material = new Material();
      material.setMaterialID(6251);
      material.setManufacturerID(14);
      material.setMaterialName("Cisco 1721 Modular Access Router");
      material.setMaterialType(AssetCategory.Networks);
      materialMap.put(6251, material);
      HashMap<Integer,Manufacturer> manufacturerMap = new HashMap<Integer,Manufacturer>();
      Manufacturer manufacturer = new Manufacturer();
      manufacturer.setManufacturerID(14);
      manufacturer.setDetail("Cisco");
      manufacturerMap.put(14, manufacturer);
      Mockito.when(this.wormholeAPIClient.getAllAssetsBySourceAndType("l9i8728d55368540fcba1692",AssetCategory.Server))
      .thenReturn(new ArrayList<Asset>());
      HashMap<Long,String> chassisMountedAssetNumberAndChassisIDMap = new HashMap<Long,String>();
      chassisMountedAssetNumberAndChassisIDMap.put(197L, "asdqe945kjsdf09uw45ms");
      List<Asset>assets = nlyteDataService.generateAssets("l9i8728d55368540fcba1692", nlyteAssets,
            locationMap, manufacturerMap, materialMap, AssetCategory.Networks, chassisMountedAssetNumberAndChassisIDMap);

      for(Asset asset:assets) {
         if("sin2-blrqeops-esxstress024".equals(asset.getAssetName())) {
            TestCase.assertEquals(197, asset.getAssetNumber());
            TestCase.assertEquals("SG-07-04", asset.getRoom());
            TestCase.assertEquals("Cisco 1721 Modular Access Router", asset.getModel());
            TestCase.assertEquals("Cisco", asset.getManufacturer());
            TestCase.assertEquals("asdqe945kjsdf09uw45ms", asset.getParent().getParentId());
         }
      }
   }

   @Test
   public void testGetgetNlyteMappedAsset() {
      List<Asset> assets = Arrays.asList(getMappedAsset().getBody());
      List<Asset> mappedAsseted = nlyteDataService.getNlyteMappedAsset(assets, "");
      TestCase.assertEquals(0, mappedAsseted.size());
   }

   @Test
   public void testGetgetNlyteMappedAsset1() {
      List<Asset> assets = Arrays.asList(getMappedAsset().getBody());
      List<Asset> mappedAsseted =
            nlyteDataService.getNlyteMappedAsset(assets, "l9i8728d55368540fcba1692");
      TestCase.assertEquals(12345, mappedAsseted.get(0).getAssetNumber());
   }

   @Test
   public void testGetAssetIdfromformular() {
      List<Asset> assets = new ArrayList<Asset>();
      Set<String>assetIds = nlyteDataService.getAssetIdfromformular(assets);
      TestCase.assertEquals(0, assetIds.size());
   }

   @Test
   public void testGetAssetIdfromformular1() {
      List<Asset> assets = new ArrayList<Asset>();
      Asset asset = createAsset();
      assets.add(asset);
      Set<String>assetIds = nlyteDataService.getAssetIdfromformular(assets);
      TestCase.assertEquals(0, assetIds.size());
   }

   @Test
   public void testGetAssetIdfromformular2() {
      List<Asset> assets = Arrays.asList(getMappedAsset().getBody());
      Set<String>assetIds = nlyteDataService.getAssetIdfromformular(assets);
      TestCase.assertEquals("5x4ff46982db22e1b040e0f2", assetIds.iterator().next());
   }

   @Test
   public void testGenerateValueUnits() {
      List<PowerStripsRealtimeValue> values = getPowerStripsRealtimeValue().getBody().getValue();
      HashMap<AdvanceSettingType, String> advanceSettingMap = new HashMap<AdvanceSettingType, String>();
      advanceSettingMap.put(AdvanceSettingType.DateFormat, NlyteDataService.DateFormat);
      advanceSettingMap.put(AdvanceSettingType.TimeZone, "GMT");
      advanceSettingMap.put(AdvanceSettingType.HUMIDITY_UNIT, "%");
      advanceSettingMap.put(AdvanceSettingType.PDU_AMPS_UNIT, "A");
      advanceSettingMap.put(AdvanceSettingType.PDU_POWER_UNIT, "KW");
      advanceSettingMap.put(AdvanceSettingType.PDU_VOLT_UNIT, "V");
      advanceSettingMap.put(AdvanceSettingType.TEMPERATURE_UNIT, "C");
      List<ValueUnit> valueunits = nlyteDataService.generateValueUnits(values,advanceSettingMap);
      TestCase.assertEquals(3, valueunits.size());
      TestCase.assertEquals(1537454125000l, valueunits.get(0).getTime());
      TestCase.assertEquals(20.0, valueunits.get(0).getValueNum());
      TestCase.assertEquals(20.0, valueunits.get(1).getValueNum());
      TestCase.assertEquals(120.0, valueunits.get(2).getValueNum());

   }

   @Test
   public void testGenerateValueUnits2() {
      List<PowerStripsRealtimeValue> values = getPowerStripsRealtimeValue1().getBody().getValue();
      HashMap<AdvanceSettingType, String> advanceSettingMap = new HashMap<AdvanceSettingType, String>();
      advanceSettingMap.put(AdvanceSettingType.DateFormat, NlyteDataService.DateFormat);
      advanceSettingMap.put(AdvanceSettingType.TimeZone, "GMT");
      advanceSettingMap.put(AdvanceSettingType.HUMIDITY_UNIT, "%");
      advanceSettingMap.put(AdvanceSettingType.PDU_AMPS_UNIT, "A");
      advanceSettingMap.put(AdvanceSettingType.PDU_POWER_UNIT, "KW");
      advanceSettingMap.put(AdvanceSettingType.PDU_VOLT_UNIT, "V");
      advanceSettingMap.put(AdvanceSettingType.TEMPERATURE_UNIT, "C");
      List<ValueUnit> valueunits = nlyteDataService.generateValueUnits(values,advanceSettingMap);
      TestCase.assertEquals(3, valueunits.size());
      TestCase.assertEquals(1537454125000l, valueunits.get(0).getTime());
      TestCase.assertEquals(20.0, valueunits.get(0).getValueNum());
      TestCase.assertEquals(20.0, valueunits.get(1).getValueNum());
      TestCase.assertEquals(120.0, valueunits.get(2).getValueNum());

   }

   @Test
   public void testGenerateValueUnits1() {
      List<PowerStripsRealtimeValue> values = new ArrayList<PowerStripsRealtimeValue>();
      HashMap<AdvanceSettingType, String> advanceSettingMap = new HashMap<AdvanceSettingType, String>();
      List<ValueUnit> valueunits = nlyteDataService.generateValueUnits(values,advanceSettingMap);
      TestCase.assertEquals(0, valueunits.size());
   }

   @Test
   public void testGenerateRealTimeData() {
      Asset asset = createAsset();
      asset.setId("5x4ff46982db22e1b040e0f2");
      HashMap<AdvanceSettingType, String> advanceSettingMap = new HashMap<AdvanceSettingType, String>();
      advanceSettingMap.put(AdvanceSettingType.DateFormat, NlyteDataService.DateFormat);
      advanceSettingMap.put(AdvanceSettingType.TimeZone, "GMT");
      RealTimeData data =  nlyteDataService.generateRealTimeData(asset, nlyteAPIClient,advanceSettingMap);
      TestCase.assertEquals("5x4ff46982db22e1b040e0f2", data.getAssetID());
   }

   @Test
   public void testGetRealTimeDatas() {
      Set<String> assetIds = new HashSet<String>();
      assetIds.add("5x4ff46982db22e1b040e0f2");
      FacilitySoftwareConfig config = getFacilitySoftwareByType().getBody()[0];
      List<RealTimeData> realTimeDatas =
            nlyteDataService.getRealTimeDatas(nlyteAPIClient, config, assetIds);
      TestCase.assertEquals(1, realTimeDatas.size());
      TestCase.assertEquals("5x4ff46982db22e1b040e0f2", realTimeDatas.get(0).getAssetID());
   }

   @Test
   public void testHandleChassisSolts() {
      Asset asset = createAsset();
      List<ChassisSlot> chassisSolts = new ArrayList<ChassisSlot>();
      NlyteAsset nlyteAsset = getNlyteAsset().get(0);
      HandleAssetUtil util = new HandleAssetUtil();
      util.handleChassisSolts(asset, nlyteAsset);
      TestCase.assertEquals(true, asset.getJustificationfields().isEmpty());
   }

   @Test
   public void testHandleChassisSolts1() {
      Asset asset = createAsset();
      List<ChassisSlot> chassisSolts = new ArrayList<ChassisSlot>();
      ChassisSlot slot = new ChassisSlot();
      slot.setChassisAssetID(105);
      slot.setColumnPosition(1);
      slot.setRowPosition(1);
      slot.setMountingSide("Front");
      slot.setSlotName("1");
      slot.setId(78);
      chassisSolts.add(slot);
      ChassisSlot slot2 = new ChassisSlot();
      slot2.setChassisAssetID(105);
      slot2.setColumnPosition(1);
      slot2.setRowPosition(2);
      slot2.setMountingSide("Back");
      slot2.setSlotName("2");
      slot2.setId(79);
      chassisSolts.add(slot2);
      NlyteAsset nlyteAsset = getNlyteAsset().get(0);
      nlyteAsset.setChassisSlots(chassisSolts);
      HandleAssetUtil util = new HandleAssetUtil();
      util.handleChassisSolts(asset, nlyteAsset);
      String chassisInfo = asset.getJustificationfields().get(FlowgateConstant.CHASSIS);
      ObjectMapper mapper = new ObjectMapper();
      try {
         Map<String, String> chassisInfoMap = mapper.readValue(chassisInfo, new TypeReference<Map<String,String>>() {});
         List<FlowgateChassisSlot> slots = mapper.readValue(chassisInfoMap.get(FlowgateConstant.CHASSISSLOTS), new TypeReference<List<FlowgateChassisSlot>>() {});
         for(FlowgateChassisSlot chassisslot : slots) {
            if(chassisslot.getSlotName().equals("1")) {
               TestCase.assertEquals("Front", chassisslot.getMountingSide());
            }else if(chassisslot.getSlotName().equals("2")) {
               TestCase.assertEquals("Back", chassisslot.getMountingSide());
            }else {
               TestCase.fail();
            }
         }
      } catch (JsonProcessingException e) {
        TestCase.fail(e.getMessage());
      }
   }

   @Test
   public void testHandleChassisSolts2() {
      Asset asset = createAsset();
      List<ChassisSlot> chassisSolts = new ArrayList<ChassisSlot>();
      ChassisSlot slot = new ChassisSlot();
      slot.setChassisAssetID(105);
      slot.setColumnPosition(1);
      slot.setRowPosition(1);
      slot.setMountingSide("Front");
      slot.setSlotName("1");
      slot.setId(78);
      chassisSolts.add(slot);
      ChassisSlot slot2 = new ChassisSlot();
      slot2.setChassisAssetID(105);
      slot2.setColumnPosition(1);
      slot2.setRowPosition(2);
      slot2.setMountingSide("Back");
      slot2.setSlotName("2");
      slot2.setId(79);
      chassisSolts.add(slot2);
      List<ChassisMountedAssetMap> cmAssets = new ArrayList<ChassisMountedAssetMap>();
      ChassisMountedAssetMap cmAsset = new ChassisMountedAssetMap();
      cmAsset.setMountedAssetID(197);
      cmAsset.setMountingSide("Front");
      cmAsset.setSlotName("1");
      cmAssets.add(cmAsset);
      ChassisMountedAssetMap cmAsset2 = new ChassisMountedAssetMap();
      cmAsset2.setMountedAssetID(198);
      cmAsset2.setMountingSide("Back");
      cmAsset2.setSlotName("2");
      cmAssets.add(cmAsset2);
      NlyteAsset nlyteAsset = getNlyteAsset().get(0);
      nlyteAsset.setChassisSlots(chassisSolts);
      nlyteAsset.setChassisMountedAssetMaps(cmAssets);
      HandleAssetUtil util = new HandleAssetUtil();
      util.handleChassisSolts(asset, nlyteAsset);
      String chassisInfo = asset.getJustificationfields().get(FlowgateConstant.CHASSIS);
      ObjectMapper mapper = new ObjectMapper();
      try {
         Map<String, String> chassisInfoMap = mapper.readValue(chassisInfo, new TypeReference<Map<String,String>>() {});
         List<FlowgateChassisSlot> slots = mapper.readValue(chassisInfoMap.get(FlowgateConstant.CHASSISSLOTS), new TypeReference<List<FlowgateChassisSlot>>() {});
         for(FlowgateChassisSlot chassisslot : slots) {
            if(chassisslot.getSlotName().equals("1")) {
               TestCase.assertEquals("Front", chassisslot.getMountingSide());
               TestCase.assertEquals(Integer.valueOf(197), chassisslot.getMountedAssetNumber());
            }else if(chassisslot.getSlotName().equals("2")) {
               TestCase.assertEquals("Back", chassisslot.getMountingSide());
               TestCase.assertEquals(Integer.valueOf(198), chassisslot.getMountedAssetNumber());
            }else {
               TestCase.fail();
            }
         }
      } catch (JsonProcessingException e) {
        TestCase.fail(e.getMessage());
      }
   }

   @Test
   public void testHandleChassisSolts3() {
      Asset asset = createAsset();
      List<ChassisMountedAssetMap> chassisMaps = new ArrayList<ChassisMountedAssetMap>();
      ChassisMountedAssetMap cMap1 = new ChassisMountedAssetMap();
      cMap1.setMountedAssetID(105);
      cMap1.setColumnPosition(1);
      cMap1.setRowPosition(1);
      cMap1.setMountingSide("Front");
      cMap1.setSlotName("1");
      chassisMaps.add(cMap1);
      ChassisMountedAssetMap cMap2 = new ChassisMountedAssetMap();
      cMap2.setMountedAssetID(198);
      cMap2.setColumnPosition(1);
      cMap2.setRowPosition(2);
      cMap2.setMountingSide("Back");
      cMap2.setSlotName("2");
      chassisMaps.add(cMap2);
      NlyteAsset nlyteAsset = getNlyteAsset().get(0);
      nlyteAsset.setChassisMountedAssetMaps(chassisMaps);
      HandleAssetUtil util = new HandleAssetUtil();
      util.handleChassisSolts(asset, nlyteAsset);
      String chassisInfo = asset.getJustificationfields().get(FlowgateConstant.CHASSIS);
      ObjectMapper mapper = new ObjectMapper();
      try {
         Map<String, String> chassisInfoMap = mapper.readValue(chassisInfo, new TypeReference<Map<String,String>>() {});
         List<FlowgateChassisSlot> slots = mapper.readValue(chassisInfoMap.get(FlowgateConstant.CHASSISSLOTS), new TypeReference<List<FlowgateChassisSlot>>() {});
         for(FlowgateChassisSlot chassisslot : slots) {
            if(chassisslot.getSlotName().equals("1")) {
               TestCase.assertEquals("Front", chassisslot.getMountingSide());
               TestCase.assertEquals(Integer.valueOf(105), chassisslot.getMountedAssetNumber());
            }else if(chassisslot.getSlotName().equals("2")) {
               TestCase.assertEquals("Back", chassisslot.getMountingSide());
               TestCase.assertEquals(Integer.valueOf(198), chassisslot.getMountedAssetNumber());
            }else {
               TestCase.fail();
            }
         }
      } catch (JsonProcessingException e) {
        TestCase.fail(e.getMessage());
      }
   }

   @Test
   public void testSupplementChassisInfo() {
      Asset asset = createAsset();
      HandleAssetUtil util = new HandleAssetUtil();
      util.supplementChassisInfo(asset, FlowgateConstant.CHASSIS_AIR_FLOW_TYPE, null);
      TestCase.assertEquals(true, asset.getJustificationfields().isEmpty());
   }

   @Test
   public void testSupplementChassisInfo1() {
      Asset asset = createAsset();
      HashMap<String,String> justficationMap = new HashMap<String,String>();
      Map<String, String> chassisInfoMap = new HashMap<String,String>();
      ObjectMapper mapper = new ObjectMapper();
      try {
         chassisInfoMap.put(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE, "frontToBack");
         String chassisInfo = mapper.writeValueAsString(chassisInfoMap);
         justficationMap.put(FlowgateConstant.CHASSIS, chassisInfo);
         asset.setJustificationfields(justficationMap);
         HandleAssetUtil util = new HandleAssetUtil();
         util.supplementChassisInfo(asset, FlowgateConstant.CHASSIS_AIR_FLOW_TYPE, "backToFront");

         justficationMap = asset.getJustificationfields();
         chassisInfo = justficationMap.get(FlowgateConstant.CHASSIS);
         chassisInfoMap = mapper.readValue(chassisInfo, new TypeReference<Map<String,String>>() {});
         TestCase.assertEquals("backToFront", chassisInfoMap.get(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE));
      } catch (Exception e) {
         TestCase.fail(e.getMessage());
      }
   }

   @Test
   public void testCompareValueIsNotEqual() {
      String value1 = null;
      String value2 = null;
      HandleAssetUtil util = new HandleAssetUtil();
      TestCase.assertEquals(false, util.valueIsChanged(value1, value2));
   }

   @Test
   public void testCompareValueIsNotEqual2() {
      Integer value1 = null;
      Integer value2 = null;
      HandleAssetUtil util = new HandleAssetUtil();
      TestCase.assertEquals(false, util.valueIsChanged(value1, value2));
   }

   @Test
   public void testChassisSlotsIsChanged() {
      List<FlowgateChassisSlot> oldFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot = createFlowgateChassisSlot();
      oldFlowgateChassisSlots.add(slot);

      List<FlowgateChassisSlot> newFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot1 = createFlowgateChassisSlot();
      newFlowgateChassisSlots.add(slot1);

      HandleAssetUtil util = new HandleAssetUtil();
      TestCase.assertEquals(false, util.chassisSlotsIsChanged(oldFlowgateChassisSlots, newFlowgateChassisSlots));
   }

   @Test
   public void testChassisSlotsIsChanged2() {
      List<FlowgateChassisSlot> oldFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot = createFlowgateChassisSlot();
      oldFlowgateChassisSlots.add(slot);

      HandleAssetUtil util = new HandleAssetUtil();
      TestCase.assertEquals(true, util.chassisSlotsIsChanged(oldFlowgateChassisSlots, new ArrayList<FlowgateChassisSlot>()));
   }

   @Test
   public void testChassisSlotsIsChanged3() {
      List<FlowgateChassisSlot> oldFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot = createFlowgateChassisSlot();
      oldFlowgateChassisSlots.add(slot);

      List<FlowgateChassisSlot> newFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot1 = createFlowgateChassisSlot();
      slot1.setMountingSide("front");
      newFlowgateChassisSlots.add(slot1);
      HandleAssetUtil util = new HandleAssetUtil();
      TestCase.assertEquals(true, util.chassisSlotsIsChanged(oldFlowgateChassisSlots, newFlowgateChassisSlots));
   }

   @Test
   public void testChassisSlotsIsChanged4() {
      List<FlowgateChassisSlot> oldFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot = createFlowgateChassisSlot();
      oldFlowgateChassisSlots.add(slot);

      List<FlowgateChassisSlot> newFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot1 = createFlowgateChassisSlot();
      slot1.setMountedAssetNumber(125);
      newFlowgateChassisSlots.add(slot1);
      HandleAssetUtil util = new HandleAssetUtil();
      TestCase.assertEquals(true, util.chassisSlotsIsChanged(oldFlowgateChassisSlots, newFlowgateChassisSlots));
   }

   @Test
   public void testChassisSlotsIsChanged5() {
      List<FlowgateChassisSlot> oldFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot = createFlowgateChassisSlot();
      oldFlowgateChassisSlots.add(slot);

      List<FlowgateChassisSlot> newFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot1 = createFlowgateChassisSlot();
      slot1.setColumnPosition(2);
      newFlowgateChassisSlots.add(slot1);
      HandleAssetUtil util = new HandleAssetUtil();
      TestCase.assertEquals(true, util.chassisSlotsIsChanged(oldFlowgateChassisSlots, newFlowgateChassisSlots));
   }

   @Test
   public void testChassisSlotsIsChanged6() {
      List<FlowgateChassisSlot> oldFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot = createFlowgateChassisSlot();
      oldFlowgateChassisSlots.add(slot);

      List<FlowgateChassisSlot> newFlowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot1 = createFlowgateChassisSlot();
      slot1.setRowPosition(2);
      newFlowgateChassisSlots.add(slot1);
      HandleAssetUtil util = new HandleAssetUtil();
      TestCase.assertEquals(true, util.chassisSlotsIsChanged(oldFlowgateChassisSlots, newFlowgateChassisSlots));
   }

   /**
    * New asset have chassis info but old assets none
    *
    */
   @Test
   public void testHandleAssets() {
      List<Asset> toUpdateAssets = new ArrayList<Asset>();
      Map<Long,Asset> exsitingaAssetMap = new HashMap<Long,Asset>();
      Asset asset = createAsset();
      asset.setAssetNumber(127);
      asset.setTag("tag1");
      asset.setCapacity(8);
      asset.setFreeCapacity(2);
      toUpdateAssets.add(asset);
      Asset asset2 = createAsset();
      asset2.setAssetNumber(128);
      asset2.setTag("tag2");
      asset2.setCabinetName("cabinet1");
      Tenant tenant = new Tenant();
      tenant.setOwner("admin");
      tenant.setTenant("tenant");
      tenant.setTenantManager("manager");
      asset2.setTenant(tenant);
      asset2.setRoom("room1");
      asset2.setRow("r2");
      asset2.setCol("c2");
      asset2.setMountingSide(MountingSide.Front);
      asset2.setCategory(AssetCategory.Chassis);
      HashMap<String,String> justficationMap = new HashMap<String,String>();
      List<FlowgateChassisSlot> slots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot1 = createFlowgateChassisSlot();
      slots.add(slot1);
      Map<String,String> chassisInfoMap = new HashMap<String,String>();
      chassisInfoMap.put(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE, "frontToBack");
      ObjectMapper mapper = new ObjectMapper();
      try {
         String slotsString = mapper.writeValueAsString(slots);
         chassisInfoMap.put(FlowgateConstant.CHASSISSLOTS, slotsString);
         String chassisInfo = mapper.writeValueAsString(chassisInfoMap);
         justficationMap.put(FlowgateConstant.CHASSIS, chassisInfo);
         asset2.setJustificationfields(justficationMap);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      toUpdateAssets.add(asset2);

      Asset oldAsset1 = createAsset();
      oldAsset1.setAssetNumber(127);
      oldAsset1.setTag("oldtag1");
      exsitingaAssetMap.put(oldAsset1.getAssetNumber(), oldAsset1);
      Asset oldAsset2 = createAsset();
      oldAsset2.setAssetNumber(128);
      oldAsset2.setTag("oldtag2");
      exsitingaAssetMap.put(oldAsset2.getAssetNumber(), oldAsset2);
      HandleAssetUtil util = new HandleAssetUtil();
      List<Asset> assets = util.handleAssets(toUpdateAssets, exsitingaAssetMap);
      for (Asset assetTosave : assets) {
         if (assetTosave.getAssetNumber() == 128) {
            TestCase.assertEquals(asset2.getTag(), assetTosave.getTag());
            TestCase.assertEquals(asset2.getRoom(), assetTosave.getRoom());
            TestCase.assertEquals(asset2.getRow(), assetTosave.getRow());
            TestCase.assertEquals(asset2.getCol(), assetTosave.getCol());
            TestCase.assertEquals(asset2.getMountingSide(), assetTosave.getMountingSide());
            TestCase.assertEquals(asset2.getTenant().getOwner(),
                  assetTosave.getTenant().getOwner());
            HashMap<String, String> justfications = assetTosave.getJustificationfields();
            String chassisInfo = justfications.get(FlowgateConstant.CHASSIS);
            try {
               Map<String, String> newChassisInfoMap =
                     mapper.readValue(chassisInfo, new TypeReference<Map<String, String>>() {
                     });
               TestCase.assertEquals(chassisInfoMap.get(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE),
                     newChassisInfoMap.get(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE));
               String chassisSlots = newChassisInfoMap.get(FlowgateConstant.CHASSISSLOTS);
               List<FlowgateChassisSlot> flowgateSlots = mapper.readValue(chassisSlots, new TypeReference<List<FlowgateChassisSlot>>() {});
               TestCase.assertEquals(slot1.getMountingSide(), flowgateSlots.get(0).getMountingSide());
               TestCase.assertEquals(slot1.getMountedAssetNumber(), flowgateSlots.get(0).getMountedAssetNumber());
               TestCase.assertEquals(slot1.getColumnPosition(), flowgateSlots.get(0).getColumnPosition());
               TestCase.assertEquals(slot1.getRowPosition(), flowgateSlots.get(0).getRowPosition());
               TestCase.assertEquals(slot1.getSlotName(), flowgateSlots.get(0).getSlotName());
            } catch (Exception e) {
               TestCase.fail(e.getMessage());
            }
         }else if(assetTosave.getAssetNumber() == 127) {
            TestCase.assertEquals(asset.getTag(), assetTosave.getTag());
            TestCase.assertEquals(asset.getCapacity(), assetTosave.getCapacity());
            TestCase.assertEquals(asset.getFreeCapacity(), assetTosave.getFreeCapacity());
         }else {
            TestCase.fail("Invalid assetNumber");
         }
      }
   }

   /**
    * CHASSIS_AIR_FLOW_TYPE changed
    */
   @Test
   public void testHandleAssets1() {
      List<Asset> toUpdateAssets = new ArrayList<Asset>();
      Map<Long,Asset> exsitingaAssetMap = new HashMap<Long,Asset>();
      Asset asset = createAsset();
      asset.setAssetNumber(127);
      asset.setTag("tag1");
      toUpdateAssets.add(asset);
      Asset asset2 = createAsset();
      asset2.setAssetNumber(128);
      asset2.setTag("tag2");
      asset2.setCabinetName("cabinet1");
      Tenant tenant = new Tenant();
      tenant.setOwner("admin");
      tenant.setTenant("tenant");
      tenant.setTenantManager("manager");
      asset2.setTenant(tenant);
      asset2.setRoom("room1");
      asset2.setRow("r2");
      asset2.setCol("c2");
      asset2.setMountingSide(MountingSide.Front);
      asset2.setCategory(AssetCategory.Chassis);
      HashMap<String,String> justficationMap = new HashMap<String,String>();
      List<FlowgateChassisSlot> slots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot1 = createFlowgateChassisSlot();
      slots.add(slot1);
      Map<String,String> chassisInfoMap = new HashMap<String,String>();
      chassisInfoMap.put(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE, "frontToBack");
      ObjectMapper mapper = new ObjectMapper();
      try {
         String slotsString = mapper.writeValueAsString(slots);
         chassisInfoMap.put(FlowgateConstant.CHASSISSLOTS, slotsString);
         String chassisInfo = mapper.writeValueAsString(chassisInfoMap);
         justficationMap.put(FlowgateConstant.CHASSIS, chassisInfo);
         asset2.setJustificationfields(justficationMap);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      toUpdateAssets.add(asset2);

      Asset oldAsset1 = createAsset();
      oldAsset1.setAssetNumber(127);
      oldAsset1.setTag("oldtag1");
      exsitingaAssetMap.put(oldAsset1.getAssetNumber(), oldAsset1);
      Asset oldAsset2 = createAsset();
      oldAsset2.setAssetNumber(128);
      oldAsset2.setTag("oldtag2");
      HashMap<String,String> oldjustficationMap = new HashMap<String,String>();
      List<FlowgateChassisSlot> oldslots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot oldslot1 = createFlowgateChassisSlot();
      oldslot1.setMountingSide("Back");
      oldslot1.setMountedAssetNumber(5523);
      oldslots.add(oldslot1);
      Map<String,String> oldChassisInfoMap = new HashMap<String,String>();
      oldChassisInfoMap.put(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE, "leftToRight");
      try {
         String oldSlotsString = mapper.writeValueAsString(oldslots);
         oldChassisInfoMap.put(FlowgateConstant.CHASSISSLOTS, oldSlotsString);
         String oldChassisInfo = mapper.writeValueAsString(oldChassisInfoMap);
         oldjustficationMap.put(FlowgateConstant.CHASSIS, oldChassisInfo);
         oldAsset2.setJustificationfields(oldjustficationMap);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      exsitingaAssetMap.put(oldAsset2.getAssetNumber(), oldAsset2);
      HandleAssetUtil util = new HandleAssetUtil();
      List<Asset> assets = util.handleAssets(toUpdateAssets, exsitingaAssetMap);
      for (Asset assetTosave : assets) {
         if (assetTosave.getAssetNumber() == 128) {
            TestCase.assertEquals(asset2.getTag(), assetTosave.getTag());
            TestCase.assertEquals(asset2.getRoom(), assetTosave.getRoom());
            TestCase.assertEquals(asset2.getRow(), assetTosave.getRow());
            TestCase.assertEquals(asset2.getCol(), assetTosave.getCol());
            TestCase.assertEquals(asset2.getMountingSide(), assetTosave.getMountingSide());
            TestCase.assertEquals(asset2.getTenant().getOwner(),
                  assetTosave.getTenant().getOwner());
            HashMap<String, String> justfications = assetTosave.getJustificationfields();
            String chassisInfo = justfications.get(FlowgateConstant.CHASSIS);
            try {
               Map<String, String> newChassisInfoMap =
                     mapper.readValue(chassisInfo, new TypeReference<Map<String, String>>() {
                     });
               TestCase.assertEquals(chassisInfoMap.get(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE),
                     newChassisInfoMap.get(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE));
               String chassisSlots = newChassisInfoMap.get(FlowgateConstant.CHASSISSLOTS);
               List<FlowgateChassisSlot> flowgateSlots = mapper.readValue(chassisSlots, new TypeReference<List<FlowgateChassisSlot>>() {});
               TestCase.assertEquals(slot1.getMountingSide(), flowgateSlots.get(0).getMountingSide());
               TestCase.assertEquals(slot1.getMountedAssetNumber(), flowgateSlots.get(0).getMountedAssetNumber());
               TestCase.assertEquals(slot1.getColumnPosition(), flowgateSlots.get(0).getColumnPosition());
               TestCase.assertEquals(slot1.getRowPosition(), flowgateSlots.get(0).getRowPosition());
               TestCase.assertEquals(slot1.getSlotName(), flowgateSlots.get(0).getSlotName());
            } catch (Exception e) {
               TestCase.fail(e.getMessage());
            }
         }else if(assetTosave.getAssetNumber() == 127) {
            TestCase.assertEquals(asset.getTag(), assetTosave.getTag());
         }else {
            TestCase.fail("Invalid assetNumber");
         }
      }
   }

   @Test
   public void testHandleAssets2() {
      List<Asset> toUpdateAssets = new ArrayList<Asset>();
      Map<Long,Asset> exsitingaAssetMap = new HashMap<Long,Asset>();
      Asset asset = createAsset();
      asset.setAssetNumber(127);
      asset.setTag("tag1");
      toUpdateAssets.add(asset);
      Asset asset2 = createAsset();
      asset2.setAssetNumber(128);
      asset2.setTag("tag2");
      asset2.setCabinetName("cabinet1");
      Tenant tenant = new Tenant();
      tenant.setOwner("admin");
      tenant.setTenant("tenant");
      tenant.setTenantManager("manager");
      asset2.setTenant(tenant);
      asset2.setRoom("room1");
      asset2.setRow("r2");
      asset2.setCol("c2");
      asset2.setMountingSide(MountingSide.Front);
      asset2.setCategory(AssetCategory.Chassis);
      HashMap<String,String> justficationMap = new HashMap<String,String>();
      List<FlowgateChassisSlot> slots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot1 = createFlowgateChassisSlot();
      slots.add(slot1);
      Map<String,String> chassisInfoMap = new HashMap<String,String>();
      chassisInfoMap.put(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE, "frontToBack");
      ObjectMapper mapper = new ObjectMapper();
      try {
         String slotsString = mapper.writeValueAsString(slots);
         chassisInfoMap.put(FlowgateConstant.CHASSISSLOTS, slotsString);
         String chassisInfo = mapper.writeValueAsString(chassisInfoMap);
         justficationMap.put(FlowgateConstant.CHASSIS, chassisInfo);
         asset2.setJustificationfields(justficationMap);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      toUpdateAssets.add(asset2);

      Asset oldAsset1 = createAsset();
      oldAsset1.setAssetNumber(127);
      oldAsset1.setTag("oldtag1");
      exsitingaAssetMap.put(oldAsset1.getAssetNumber(), oldAsset1);
      Asset oldAsset2 = createAsset();
      oldAsset2.setAssetNumber(128);
      oldAsset2.setTag("oldtag2");
      HashMap<String,String> oldjustficationMap = new HashMap<String,String>();
      List<FlowgateChassisSlot> oldslots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot oldslot1 = createFlowgateChassisSlot();
      oldslot1.setMountingSide("Back");
      oldslot1.setMountedAssetNumber(5523);
      oldslots.add(oldslot1);
      Map<String,String> oldChassisInfoMap = new HashMap<String,String>();
      oldChassisInfoMap.put(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE, "frontToBack");
      try {
         String oldSlotsString = mapper.writeValueAsString(oldslots);
         oldChassisInfoMap.put(FlowgateConstant.CHASSISSLOTS, oldSlotsString);
         String oldChassisInfo = mapper.writeValueAsString(oldChassisInfoMap);
         oldjustficationMap.put(FlowgateConstant.CHASSIS, oldChassisInfo);
         oldAsset2.setJustificationfields(oldjustficationMap);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      exsitingaAssetMap.put(oldAsset2.getAssetNumber(), oldAsset2);
      HandleAssetUtil util = new HandleAssetUtil();
      List<Asset> assets = util.handleAssets(toUpdateAssets, exsitingaAssetMap);
      for (Asset assetTosave : assets) {
         if (assetTosave.getAssetNumber() == 128) {
            TestCase.assertEquals(asset2.getTag(), assetTosave.getTag());
            TestCase.assertEquals(asset2.getRoom(), assetTosave.getRoom());
            TestCase.assertEquals(asset2.getRow(), assetTosave.getRow());
            TestCase.assertEquals(asset2.getCol(), assetTosave.getCol());
            TestCase.assertEquals(asset2.getMountingSide(), assetTosave.getMountingSide());
            TestCase.assertEquals(asset2.getTenant().getOwner(),
                  assetTosave.getTenant().getOwner());
            HashMap<String, String> justfications = assetTosave.getJustificationfields();
            String chassisInfo = justfications.get(FlowgateConstant.CHASSIS);
            try {
               Map<String, String> newChassisInfoMap =
                     mapper.readValue(chassisInfo, new TypeReference<Map<String, String>>() {
                     });
               TestCase.assertEquals(chassisInfoMap.get(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE),
                     newChassisInfoMap.get(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE));
               String chassisSlots = newChassisInfoMap.get(FlowgateConstant.CHASSISSLOTS);
               List<FlowgateChassisSlot> flowgateSlots = mapper.readValue(chassisSlots, new TypeReference<List<FlowgateChassisSlot>>() {});
               TestCase.assertEquals(slot1.getMountingSide(), flowgateSlots.get(0).getMountingSide());
               TestCase.assertEquals(slot1.getMountedAssetNumber(), flowgateSlots.get(0).getMountedAssetNumber());
               TestCase.assertEquals(slot1.getColumnPosition(), flowgateSlots.get(0).getColumnPosition());
               TestCase.assertEquals(slot1.getRowPosition(), flowgateSlots.get(0).getRowPosition());
               TestCase.assertEquals(slot1.getSlotName(), flowgateSlots.get(0).getSlotName());
            } catch (Exception e) {
               TestCase.fail(e.getMessage());
            }
         }else if(assetTosave.getAssetNumber() == 127) {
            TestCase.assertEquals(asset.getTag(), assetTosave.getTag());
         }else {
            TestCase.fail("Invalid assetNumber");
         }
      }
   }

   @Test
   public void testHandleAssets3() {
      List<Asset> toUpdateAssets = new ArrayList<Asset>();
      Map<Long,Asset> exsitingaAssetMap = new HashMap<Long,Asset>();
      Asset asset = createAsset();
      asset.setAssetNumber(127);
      asset.setTag("tag1");
      toUpdateAssets.add(asset);
      Asset asset2 = createAsset();
      asset2.setAssetNumber(128);
      asset2.setTag("tag2");
      asset2.setCabinetName("cabinet1");
      Tenant tenant = new Tenant();
      tenant.setOwner("admin");
      tenant.setTenant("tenant");
      tenant.setTenantManager("manager");
      asset2.setTenant(tenant);
      asset2.setRoom("room1");
      asset2.setRow("r2");
      asset2.setCol("c2");
      asset2.setMountingSide(MountingSide.Front);
      asset2.setCategory(AssetCategory.Server);
      Parent parent = new Parent();
      parent.setParentId("ouqwenkja72hoas9034a");
      parent.setType("Chassis");
      asset2.setParent(parent);
      toUpdateAssets.add(asset2);

      Asset oldAsset1 = createAsset();
      oldAsset1.setAssetNumber(127);
      oldAsset1.setTag("oldtag1");
      exsitingaAssetMap.put(oldAsset1.getAssetNumber(), oldAsset1);
      Asset oldAsset2 = createAsset();
      oldAsset2.setAssetNumber(128);
      oldAsset2.setTag("oldtag2");
      exsitingaAssetMap.put(oldAsset2.getAssetNumber(), oldAsset2);
      HandleAssetUtil util = new HandleAssetUtil();
      List<Asset> assets = util.handleAssets(toUpdateAssets, exsitingaAssetMap);
      for (Asset assetTosave : assets) {
         if (assetTosave.getAssetNumber() == 128) {
            TestCase.assertEquals(asset2.getTag(), assetTosave.getTag());
            TestCase.assertEquals(asset2.getRoom(), assetTosave.getRoom());
            TestCase.assertEquals(asset2.getRow(), assetTosave.getRow());
            TestCase.assertEquals(asset2.getCol(), assetTosave.getCol());
            TestCase.assertEquals(asset2.getMountingSide(), assetTosave.getMountingSide());
            TestCase.assertEquals(asset2.getTenant().getOwner(),
                  assetTosave.getTenant().getOwner());
            HashMap<String, String> justfications = assetTosave.getJustificationfields();
            TestCase.assertEquals("ouqwenkja72hoas9034a", assetTosave.getParent().getParentId());
         }else if(assetTosave.getAssetNumber() == 127) {
            TestCase.assertEquals(asset.getTag(), assetTosave.getTag());
         }else {
            TestCase.fail("Invalid assetNumber");
         }
      }
   }

   @Test
   public void testHandleAssets4() {
      List<Asset> toUpdateAssets = new ArrayList<Asset>();
      Map<Long,Asset> exsitingaAssetMap = new HashMap<Long,Asset>();
      Asset asset = createAsset();
      asset.setAssetNumber(127);
      asset.setTag("tag1");
      toUpdateAssets.add(asset);
      Asset asset2 = createAsset();
      asset2.setAssetNumber(128);
      asset2.setTag("tag2");
      asset2.setCabinetName("cabinet1");
      Tenant tenant = new Tenant();
      tenant.setOwner("admin");
      tenant.setTenant("tenant");
      tenant.setTenantManager("manager");
      asset2.setTenant(tenant);
      asset2.setRoom("room1");
      asset2.setRow("r2");
      asset2.setCol("c2");
      asset2.setMountingSide(MountingSide.Front);
      asset2.setCategory(AssetCategory.Networks);
      Parent parent = new Parent();
      parent.setParentId("ouqwenkja72hoas9034a");
      parent.setType("Chassis");
      asset2.setParent(parent);
      toUpdateAssets.add(asset2);

      Asset oldAsset1 = createAsset();
      oldAsset1.setAssetNumber(127);
      oldAsset1.setTag("oldtag1");
      exsitingaAssetMap.put(oldAsset1.getAssetNumber(), oldAsset1);
      Asset oldAsset2 = createAsset();
      oldAsset2.setAssetNumber(128);
      oldAsset2.setTag("oldtag2");
      exsitingaAssetMap.put(oldAsset2.getAssetNumber(), oldAsset2);
      HandleAssetUtil util = new HandleAssetUtil();
      List<Asset> assets = util.handleAssets(toUpdateAssets, exsitingaAssetMap);
      for (Asset assetTosave : assets) {
         if (assetTosave.getAssetNumber() == 128) {
            TestCase.assertEquals(asset2.getTag(), assetTosave.getTag());
            TestCase.assertEquals(asset2.getRoom(), assetTosave.getRoom());
            TestCase.assertEquals(asset2.getRow(), assetTosave.getRow());
            TestCase.assertEquals(asset2.getCol(), assetTosave.getCol());
            TestCase.assertEquals(asset2.getMountingSide(), assetTosave.getMountingSide());
            TestCase.assertEquals(asset2.getTenant().getOwner(),
                  assetTosave.getTenant().getOwner());
            HashMap<String, String> justfications = assetTosave.getJustificationfields();
            TestCase.assertEquals("ouqwenkja72hoas9034a", assetTosave.getParent().getParentId());
         }else if(assetTosave.getAssetNumber() == 127) {
            TestCase.assertEquals(asset.getTag(), assetTosave.getTag());
         }else {
            TestCase.fail("Invalid assetNumber");
         }
      }
   }

   @Test
   public void testHandleAssetsNew() {
      List<Asset> toUpdateAssets = new ArrayList<Asset>();
      Map<Long,Asset> exsitingaAssetMap = new HashMap<Long,Asset>();
      Asset asset = createAsset();
      asset.setAssetNumber(127);
      asset.setTag("tag1");
      asset.setCapacity(8);
      asset.setFreeCapacity(2);
      toUpdateAssets.add(asset);
      HandleAssetUtil util = new HandleAssetUtil();
      List<Asset> assets = util.handleAssets(toUpdateAssets, exsitingaAssetMap);
   }

   @Test
   public void testGenerateMountedAssetNumberAndChassisAssetIdMap() {
      List<Asset> assets = new ArrayList<Asset>();
      Asset chassisAsset = createAsset();
      chassisAsset.setAssetNumber(127);
      chassisAsset.setTag("oldtag1");
      chassisAsset.setId("asdoiqawe129012fdsfpa");
      HashMap<String,String> justficationMap = new HashMap<String,String>();
      List<FlowgateChassisSlot> slots = new ArrayList<FlowgateChassisSlot>();
      FlowgateChassisSlot slot = createFlowgateChassisSlot();
      slot.setMountingSide("Back");
      slot.setMountedAssetNumber(5523);
      slots.add(slot);
      Map<String,String> chassisInfoMap = new HashMap<String,String>();
      chassisInfoMap.put(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE, "frontToBack");
      try {
         ObjectMapper mapper = new ObjectMapper();
         String slotsString = mapper.writeValueAsString(slots);
         chassisInfoMap.put(FlowgateConstant.CHASSISSLOTS, slotsString);
         String oldChassisInfo = mapper.writeValueAsString(chassisInfoMap);
         justficationMap.put(FlowgateConstant.CHASSIS, oldChassisInfo);
         chassisAsset.setJustificationfields(justficationMap);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      assets.add(chassisAsset);
      HashMap<Long,String> map = nlyteDataService.generateMountedAssetNumberAndChassisAssetIdMap(assets);
      TestCase.assertEquals(true, map.containsKey(slot.getMountedAssetNumber().longValue()));
      TestCase.assertEquals(chassisAsset.getId(), map.get(slot.getMountedAssetNumber().longValue()));
   }

   @Test
   public void testGetAssetsFromNlytePdu() {
      String nlyteSource = "3cf0f5daff8e448da449ac88d5aa9428";
      List<NlyteAsset> nlyteAssets = getNlyteAsset();
      HashMap<Integer,LocationGroup> locationMap = getLocationMap();
      HashMap<Integer,Material> materialMap = getMaterialMap();
      Material material = new Material();
      material.setMaterialID(6251);
      material.setManufacturerID(14);
      material.setMaterialName("Cisco 1721 Modular Access Router");
      material.setMaterialType(AssetCategory.Server);
      materialMap.put(6251, material);
      HashMap<Integer,Manufacturer> manufacturerMap = getManufacturerMap();
      Manufacturer manufacturer = new Manufacturer();
      manufacturer.setManufacturerID(14);
      manufacturer.setDetail("Cisco");
      manufacturerMap.put(14, manufacturer);
      HashMap<Long,String> chassisMountedAssetNumberAndChassisIdMap = null;
      HandleAssetUtil util = new HandleAssetUtil();
      List<Asset> assets = util.getAssetsFromNlyte(nlyteSource, nlyteAssets, locationMap, materialMap, manufacturerMap, chassisMountedAssetNumberAndChassisIdMap);
   }

   @Test
   public void testSavePduAssetAndUpdatePduUsageFormula() {
      List<Asset> assets = new ArrayList<>();
      Asset asset = createAsset();
      asset.setCategory(AssetCategory.PDU);
      asset.setId(null);
      assets.add(asset);

      Mockito.doReturn(new ResponseEntity<Void>(HttpStatus.OK)).when(wormholeAPIClient).saveAssets(Mockito.any(Asset.class));
      Mockito.doReturn("23551d6dacf2432c8a3edbc6bbc922cd").when(nlyteDataService).getAssetIdByResponseEntity(Mockito.any(ResponseEntity.class));

      nlyteDataService.savePduAssetAndUpdatePduUsageFormula(assets);
   }

   public FlowgateChassisSlot createFlowgateChassisSlot() {
      FlowgateChassisSlot slot = new FlowgateChassisSlot();
      slot.setMountingSide("Back");
      slot.setColumnPosition(1);
      slot.setMountedAssetNumber(127);
      slot.setRowPosition(1);
      slot.setSlotName("1");
      return slot;
   }
   public ResponseEntity<FacilitySoftwareConfig []> getFacilitySoftwareByType(){
      HashMap<AdvanceSettingType, String> advanceSettingMap = new HashMap<AdvanceSettingType, String>();
      advanceSettingMap.put(AdvanceSettingType.DateFormat, NlyteDataService.DateFormat);
      advanceSettingMap.put(AdvanceSettingType.TimeZone, "GMT");
      FacilitySoftwareConfig []configs = new FacilitySoftwareConfig[1];
      configs[0] = new FacilitySoftwareConfig();
      configs[0].setId("l9i8728d55368540fcba1692");
      configs[0].setType(SoftwareType.Nlyte);
      configs[0].setAdvanceSetting(advanceSettingMap);
      return new ResponseEntity<FacilitySoftwareConfig[]>(configs,HttpStatus.OK);
   }

   public ResponseEntity<Asset[]> getMappedAsset(){
      Asset[] assets = new Asset[2];
      assets[0] = createAsset();
      Map<String, String> formulars = new HashMap<>();

      Map<String, Map<String, String>> pduMap = new HashMap<String, Map<String, String>>();
      Map<String, String> metricNameAndId = new HashMap<String, String>();
      metricNameAndId.put(MetricName.PDU_CURRENT_LOAD, "5x4ff46982db22e1b040e0f2");
      metricNameAndId.put(MetricName.PDU_TOTAL_POWER, "5x4ff46982db22e1b040e0f2");
      metricNameAndId.put(MetricName.PDU_VOLTAGE, "5x4ff46982db22e1b040e0f2");
      pduMap.put("5x4ff46982db22e1b040e0f2", metricNameAndId);
      try {
         formulars.put(FlowgateConstant.PDU, mapper.writeValueAsString(pduMap));
      } catch (JsonProcessingException e) {
         e.printStackTrace();
      }
      assets[0].setMetricsformulars(formulars);
      assets[1] = createAsset();
      assets[1].setMetricsformulars(formulars);
      return new ResponseEntity<Asset[]>(assets,HttpStatus.OK);
   }

   public ResponseEntity<Asset> getAssetByID(){
      Asset asset = createAsset();
      asset.setId("5x4ff46982db22e1b040e0f2");
      return new ResponseEntity<Asset>(asset,HttpStatus.OK);
   }

   public ResponseEntity<JsonResultForPDURealtimeValue> getPowerStripsRealtimeValue(){
      JsonResultForPDURealtimeValue pduvalue = new JsonResultForPDURealtimeValue();
      List<PowerStripsRealtimeValue> value = new ArrayList<PowerStripsRealtimeValue>();
      PowerStripsRealtimeValue power = new PowerStripsRealtimeValue();
      power.setName("RealtimePower");
      power.setUnit("kw");
      power.setValue(20);
      power.setRecordedDateTime("2018-09-20T14:35:25Z");
      value.add(power);
      PowerStripsRealtimeValue current = new PowerStripsRealtimeValue();
      current.setName("RealtimeLoad");
      current.setUnit("Amps");
      current.setValue(20);
      current.setRecordedDateTime("2018-09-20T14:35:25Z");
      value.add(current);
      PowerStripsRealtimeValue voltage = new PowerStripsRealtimeValue();
      voltage.setName("RealtimeVoltage");
      voltage.setUnit("Volts");
      voltage.setValue(120);
      voltage.setRecordedDateTime("2018-09-20T14:35:25Z");
      value.add(voltage);
      pduvalue.setValue(value);
      return new ResponseEntity<JsonResultForPDURealtimeValue>(pduvalue,HttpStatus.OK);
   }

   public ResponseEntity<JsonResultForPDURealtimeValue> getPowerStripsRealtimeValue1(){
      JsonResultForPDURealtimeValue pduvalue = new JsonResultForPDURealtimeValue();
      List<PowerStripsRealtimeValue> value = new ArrayList<PowerStripsRealtimeValue>();
      PowerStripsRealtimeValue power = new PowerStripsRealtimeValue();
      power.setName("RealtimePower");
      power.setValue(20);
      power.setRecordedDateTime("2018-09-20T14:35:25Z");
      value.add(power);
      PowerStripsRealtimeValue current = new PowerStripsRealtimeValue();
      current.setName("RealtimeLoad");
      current.setValue(20);
      current.setUnit("");
      current.setRecordedDateTime("2018-09-20T14:35:25Z");
      value.add(current);
      PowerStripsRealtimeValue voltage = new PowerStripsRealtimeValue();
      voltage.setName("RealtimeVoltage");
      voltage.setUnit("Volts");
      voltage.setValue(120);
      voltage.setRecordedDateTime("2018-09-20T14:35:25Z");
      value.add(voltage);
      pduvalue.setValue(value);
      return new ResponseEntity<JsonResultForPDURealtimeValue>(pduvalue,HttpStatus.OK);
   }

   public ResponseEntity<Void> saveRealTimeData(){
      return new ResponseEntity<Void>(HttpStatus.OK);
   }

   Asset createAsset(){
      Asset asset = new Asset();
      asset.setAssetName("pek-wor-server-02");
      asset.setAssetNumber(12345);
      asset.setAssetSource("l9i8728d55368540fcba1692");
      asset.setCategory(AssetCategory.Server);
      asset.setModel("Dell 750");
      asset.setManufacturer("Dell");
      return asset;
   }

   String getServiceKey() {
      return "servicekey";
   }

   List<NlyteAsset> getNlyteAsset(){
      List<NlyteAsset> assets = new ArrayList<NlyteAsset>();
      NlyteAsset nlyteAsset = new NlyteAsset();
      nlyteAsset.setAssetName("sin2-blrqeops-esxstress024");
      nlyteAsset.setAssetNumber(197);
      nlyteAsset.setSerialNumber("FCH1709J3E6");
      nlyteAsset.setMaterialID(6251);
      nlyteAsset.setCabinetAssetID(562);
      nlyteAsset.setLocationGroupID(8);
      nlyteAsset.setTemplateRelated(false);
      assets.add(nlyteAsset);
      return assets;
   }

   HashMap<Integer, LocationGroup> getLocationMap(){
      HashMap<Integer, LocationGroup> locationMap = new HashMap<Integer, LocationGroup>();
      LocationGroup location = new LocationGroup();
      location.setLocationGroupID(8);
      location.setLocationGroupName("SG-07-04");
      location.setLocationGroupType("Room");
      location.setParentLocationGroupID(7);
      locationMap.put(8, location);

      LocationGroup location1 = new LocationGroup();
      location1.setLocationGroupID(7);
      location1.setLocationGroupName("4th");
      location1.setLocationGroupType("Floor");
      location1.setParentLocationGroupID(null);
      locationMap.put(7, location1);

      return locationMap;
   }

   HashMap<Integer,Material> getMaterialMap(){
      HashMap<Integer,Material> materialMap = new HashMap<Integer,Material>();
      Material material = new Material();
      material.setMaterialID(1);
      material.setManufacturerID(14);
      material.setMaterialName("Cisco 1721 Modular Access Router");
      material.setMaterialType(AssetCategory.Server);
      material.setMaterialSubtype(AssetSubCategory.Blade);
      materialMap.put(1, material);
      return materialMap;
   }

   HashMap<Integer,Manufacturer> getManufacturerMap(){
      HashMap<Integer,Manufacturer> manufacturerMap = new HashMap<Integer,Manufacturer>();
      Manufacturer manufacturer = new Manufacturer();
      manufacturer.setManufacturerID(14);
      manufacturer.setDetail("Cisco");
      return manufacturerMap;
   }


}
