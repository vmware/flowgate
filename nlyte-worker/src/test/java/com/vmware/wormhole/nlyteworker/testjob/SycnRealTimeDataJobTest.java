/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.nlyteworker.testjob;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.vmware.wormhole.client.WormholeAPIClient;
import com.vmware.wormhole.common.AssetCategory;
import com.vmware.wormhole.common.AssetSubCategory;
import com.vmware.wormhole.common.model.Asset;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.wormhole.common.model.RealTimeData;
import com.vmware.wormhole.common.model.ServerSensorData.ServerSensorType;
import com.vmware.wormhole.common.model.ValueUnit;
import com.vmware.wormhole.nlyteworker.config.ServiceKeyConfig;
import com.vmware.wormhole.nlyteworker.model.JsonResultForPDURealtimeValue;
import com.vmware.wormhole.nlyteworker.model.LocationGroup;
import com.vmware.wormhole.nlyteworker.model.Manufacturer;
import com.vmware.wormhole.nlyteworker.model.Material;
import com.vmware.wormhole.nlyteworker.model.NlyteAsset;
import com.vmware.wormhole.nlyteworker.model.PowerStripsRealtimeValue;
import com.vmware.wormhole.nlyteworker.restclient.NlyteAPIClient;
import com.vmware.wormhole.nlyteworker.scheduler.job.NlyteDataService;
import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SycnRealTimeDataJobTest {

   private static final Logger logger = LoggerFactory.getLogger(SycnRealTimeDataJobTest.class);

   @Mock
   private WormholeAPIClient wormholeAPIClient;

   @Mock
   private NlyteAPIClient nlyteAPIClient;
   
   @Mock
   private ServiceKeyConfig config;

   @Spy
   @InjectMocks
   private NlyteDataService nlyteDataService = new NlyteDataService();
   
   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
      Mockito.when(this.wormholeAPIClient.getFacilitySoftwareByType(SoftwareType.Nlyte)).thenReturn(getFacilitySoftwareByType());
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
//      try {
//         nlyteDataService.execute(null);
//      } catch (JobExecutionException e) {
//         // TODO Auto-generated catch block
//         logger.error("Test nlyteDataService get an exception ",e);
//         TestCase.fail();
//      } catch(NullPointerException e) {
//         logger.error("Test nlyteDataService get an exception ",e);
//         TestCase.fail();
//      }
   }
   
   @Test
   public void testGenerateNewAsset() {
      List<NlyteAsset> nlyteAssets = getNlyteAsset();
      HashMap<Integer, LocationGroup> locationMap = getLocationMap();
      HashMap<Integer, Material> materialMap = getMaterialMap();
      HashMap<Integer, Manufacturer> manufacturerMap = getManufacturerMap();
      Asset[] preAssets = new Asset[] {new Asset()};
      Mockito.when(this.wormholeAPIClient.getAssetsBySourceAndType("l9i8728d55368540fcba1692",
            AssetCategory.Server)).thenReturn(new ResponseEntity<Asset[]>(preAssets,HttpStatus.OK));
      
      List<Asset>assets = nlyteDataService.generateNewAsset("l9i8728d55368540fcba1692", nlyteAssets,
            locationMap, manufacturerMap, materialMap, AssetCategory.Server);
      for(Asset asset:assets) {
         if("sin2-blrqeops-esxstress024".equals(asset.getAssetName())) {
            TestCase.assertEquals(197, asset.getAssetNumber());
            TestCase.assertEquals("SG-07-04", asset.getRoom());
            TestCase.assertEquals("Cisco 1721 Modular Access Router", asset.getModel());
            TestCase.assertEquals("Cisco", asset.getManufacturer());
         }
      }
   }
   
   @Test
   public void testGenerateNewAsset1() {
      List<NlyteAsset> nlyteAssets = getNlyteAsset();
      HashMap<Integer, LocationGroup> locationMap = getLocationMap();
      HashMap<Integer, Material> materialMap = getMaterialMap();
      HashMap<Integer, Manufacturer> manufacturerMap = getManufacturerMap();
      Asset[] preAssets = new Asset[1];
      Asset preAsset = new Asset();
      preAsset.setAssetNumber(197);
      preAsset.setAssetSource("l9i8728d55368540fcba1692");
      preAssets[0] = preAsset;
      
      Mockito.when(this.wormholeAPIClient.getAssetsBySourceAndType("l9i8728d55368540fcba1692",
            AssetCategory.Server)).thenReturn(new ResponseEntity<Asset[]>(preAssets,HttpStatus.OK));
      
      List<Asset>assets = nlyteDataService.generateNewAsset("l9i8728d55368540fcba1692", nlyteAssets,
            locationMap, manufacturerMap, materialMap, AssetCategory.Server);
      for(Asset asset:assets) {
         if("sin2-blrqeops-esxstress024".equals(asset.getAssetName())) {
            TestCase.assertEquals(197, asset.getAssetNumber());
            TestCase.assertEquals("SG-07-04", asset.getRoom());
            TestCase.assertEquals("Cisco 1721 Modular Access Router", asset.getModel());
            TestCase.assertEquals("Cisco", asset.getManufacturer());
         }
      }
   }
   
   @Test
   public void testGenerateNewAsset2() {
      List<NlyteAsset> nlyteAssets = getNlyteAsset();
      HashMap<Integer, LocationGroup> locationMap = getLocationMap();
      HashMap<Integer, Material> materialMap = getMaterialMap();
      HashMap<Integer, Manufacturer> manufacturerMap = getManufacturerMap();
      Mockito.when(this.wormholeAPIClient.getAssetsBySourceAndType("l9i8728d55368540fcba1692",
            AssetCategory.Server)).thenReturn(null);
      List<Asset>assets = nlyteDataService.generateNewAsset("l9i8728d55368540fcba1692", nlyteAssets,
            locationMap, manufacturerMap, materialMap, AssetCategory.Server);
      for(Asset asset:assets) {
         if("sin2-blrqeops-esxstress024".equals(asset.getAssetName())) {
            TestCase.assertEquals(197, asset.getAssetNumber());
            TestCase.assertEquals("SG-07-04", asset.getRoom());
            TestCase.assertEquals("Cisco 1721 Modular Access Router", asset.getModel());
            TestCase.assertEquals("Cisco", asset.getManufacturer());
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
      advanceSettingMap.put(AdvanceSettingType.DateFormat, "yyyy-MM-dd'T'HH:mm:ss'Z'");
      advanceSettingMap.put(AdvanceSettingType.TimeZone, "GMT");
      List<ValueUnit> valueunits = nlyteDataService.generateValueUnits(values,advanceSettingMap);
      TestCase.assertEquals(3, valueunits.size());
      TestCase.assertEquals(1537454125000l, valueunits.get(0).getTime());
      
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
      advanceSettingMap.put(AdvanceSettingType.DateFormat, "yyyy-MM-dd'T'HH:mm:ss'Z'");
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

   public ResponseEntity<FacilitySoftwareConfig []> getFacilitySoftwareByType(){
      HashMap<AdvanceSettingType, String> advanceSettingMap = new HashMap<AdvanceSettingType, String>();
      advanceSettingMap.put(AdvanceSettingType.DateFormat, "yyyy-MM-dd'T'HH:mm:ss'Z'");
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
      EnumMap<ServerSensorType, String> sensorsformulars =
            new EnumMap<ServerSensorType, String>(ServerSensorType.class);
      sensorsformulars.put(ServerSensorType.PDU_RealtimeLoad, "5x4ff46982db22e1b040e0f2");
      sensorsformulars.put(ServerSensorType.PDU_RealtimePower, "5x4ff46982db22e1b040e0f2");
      sensorsformulars.put(ServerSensorType.PDU_RealtimeVoltage, "5x4ff46982db22e1b040e0f2");
      assets[0].setSensorsformulars(sensorsformulars);
      assets[1] = createAsset();
      assets[1].setSensorsformulars(sensorsformulars);
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
