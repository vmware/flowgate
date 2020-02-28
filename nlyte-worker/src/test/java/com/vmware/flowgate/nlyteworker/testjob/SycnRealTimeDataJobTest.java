/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.testjob;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.nlyteworker.config.ServiceKeyConfig;
import com.vmware.flowgate.nlyteworker.model.JsonResultForPDURealtimeValue;
import com.vmware.flowgate.nlyteworker.model.LocationGroup;
import com.vmware.flowgate.nlyteworker.model.Manufacturer;
import com.vmware.flowgate.nlyteworker.model.Material;
import com.vmware.flowgate.nlyteworker.model.NlyteAsset;
import com.vmware.flowgate.nlyteworker.model.PowerStripsRealtimeValue;
import com.vmware.flowgate.nlyteworker.restclient.NlyteAPIClient;
import com.vmware.flowgate.nlyteworker.scheduler.job.NlyteDataService;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SycnRealTimeDataJobTest {

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
   }

   @Test
   public void testGenerateNewAsset() {
      List<NlyteAsset> nlyteAssets = getNlyteAsset();
      HashMap<Integer, LocationGroup> locationMap = getLocationMap();
      HashMap<Integer, Material> materialMap = getMaterialMap();
      HashMap<Integer, Manufacturer> manufacturerMap = getManufacturerMap();
      Mockito.when(this.wormholeAPIClient.getAllAssetsBySourceAndType("l9i8728d55368540fcba1692",AssetCategory.Server))
      .thenReturn(new ArrayList<Asset>());

      List<Asset>assets = nlyteDataService.generateAssets("l9i8728d55368540fcba1692", nlyteAssets,
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
            locationMap, manufacturerMap, materialMap, AssetCategory.Server);
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
      Map<String, Map<String, Map<String, String>>> formulars =
            new HashMap<String, Map<String, Map<String, String>>>();

      Map<String, Map<String, String>> pduMap = new HashMap<String, Map<String, String>>();
      Map<String, String> metricNameAndId = new HashMap<String, String>();
      metricNameAndId.put(MetricName.PDU_CURRENT_LOAD, "5x4ff46982db22e1b040e0f2");
      metricNameAndId.put(MetricName.PDU_TOTAL_POWER, "5x4ff46982db22e1b040e0f2");
      metricNameAndId.put(MetricName.PDU_VOLTAGE, "5x4ff46982db22e1b040e0f2");
      pduMap.put("5x4ff46982db22e1b040e0f2", metricNameAndId);
      formulars.put(FlowgateConstant.PDU, pduMap);
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
