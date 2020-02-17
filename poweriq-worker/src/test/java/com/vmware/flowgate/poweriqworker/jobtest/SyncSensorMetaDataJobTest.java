/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.jobtest;

import static org.mockito.Matchers.any;

import java.util.ArrayList;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.utils.WormholeDateFormat;
import com.vmware.flowgate.poweriqworker.client.PowerIQAPIClient;
import com.vmware.flowgate.poweriqworker.jobs.PowerIQService;
import com.vmware.flowgate.poweriqworker.model.Aisle;
import com.vmware.flowgate.poweriqworker.model.DataCenter;
import com.vmware.flowgate.poweriqworker.model.Floor;
import com.vmware.flowgate.poweriqworker.model.Parent;
import com.vmware.flowgate.poweriqworker.model.Pdu;
import com.vmware.flowgate.poweriqworker.model.Rack;
import com.vmware.flowgate.poweriqworker.model.Room;
import com.vmware.flowgate.poweriqworker.model.Row;
import com.vmware.flowgate.poweriqworker.model.Sensor;
import com.vmware.flowgate.poweriqworker.model.SensorReading;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SyncSensorMetaDataJobTest {

   @Mock
   private WormholeAPIClient wormholeAPIClient;


   @Mock
   private PowerIQAPIClient powerIQAPIClient;

   @Spy
   @InjectMocks
   private PowerIQService powerIQService = new PowerIQService();

   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
      Mockito.when(this.wormholeAPIClient.getFacilitySoftwareByType(SoftwareType.PowerIQ))
            .thenReturn(getFacilitySoftwareByType(SoftwareType.PowerIQ));
      Mockito.when(this.wormholeAPIClient.getAllAssetsBySourceAndType("l9i8728d55368540fcba1692",
            AssetCategory.Sensors)).thenReturn(getAssets(AssetCategory.Sensors));
      Mockito.doReturn(powerIQAPIClient).when(powerIQService).createClient(any(FacilitySoftwareConfig.class));
   }

   @Test
   public void testGetAssetsFromWormhole() {
      Map<String, Asset> assetMap =
            powerIQService.getAssetsFromWormhole("l9i8728d55368540fcba1692");
      TestCase.assertEquals(true,
            assetMap.containsKey("106"));
   }

   @Test
   public void testGetSensors() {
      Mockito.when(this.powerIQAPIClient.getSensors()).thenReturn(getSensors());
      List<Sensor> sensors = powerIQService.getSensors(powerIQAPIClient);
      TestCase.assertEquals("HumiditySensor", sensors.get(0).getName());
   }

   @Test
   public void testGetRacksMap() {
      Mockito.when(this.powerIQAPIClient.getRacks()).thenReturn(getRacks());
      Map<Long, Rack> racks = powerIQService.getRacksMap(powerIQAPIClient);
      TestCase.assertEquals(true, racks.containsKey(2L));
   }

   @Test
   public void testGetRowsMap() {
      Mockito.when(this.powerIQAPIClient.getRows()).thenReturn(getRows());
      Map<Long, Row> rows = powerIQService.getRowsMap(powerIQAPIClient);
      TestCase.assertEquals(true, rows.containsKey(1L));
   }

   @Test
   public void testGetAislesMap() {
      Mockito.when(this.powerIQAPIClient.getAisles()).thenReturn(getAisles());
      Map<Long, Aisle> aisles = powerIQService.getAislesMap(powerIQAPIClient);
      TestCase.assertEquals(true, aisles.containsKey(2L));
   }

   @Test
   public void testGetRoomsMap() {
      Mockito.when(this.powerIQAPIClient.getRooms()).thenReturn(getRooms());
      Map<Long, Room> rooms = powerIQService.getRoomsMap(powerIQAPIClient);
      TestCase.assertEquals(true, rooms.containsKey(8L));
   }

   @Test
   public void testGetFloorsMap() {
      Mockito.when(this.powerIQAPIClient.getFloors()).thenReturn(getFloors());
      Map<Long, Floor> floors = powerIQService.getFloorsMap(powerIQAPIClient);
      TestCase.assertEquals(true, floors.containsKey(5L));
   }

   @Test
   public void testGetDataCentersMap() {
      Mockito.when(this.powerIQAPIClient.getDataCenters()).thenReturn(getDataCenters());
      Map<Long, DataCenter> dataCenters = powerIQService.getDataCentersMap(powerIQAPIClient);
      TestCase.assertEquals(true, dataCenters.containsKey(7L));
   }

   @Test
   public void testGetSensorRealTimeData() {
      HashMap<String, String> justificationfields = generateExtraInfo("6566");
      Asset asset = createAsset();
      asset.setId("123o89qw4jjasd0");
      asset.setJustificationfields(justificationfields);
      Sensor sensor = createSensor();
      sensor.setId(6566);
      sensor.setName("HumiditySensor");
      sensor.setSerialNumber("8999");
      sensor.setType("HumiditySensor");
      SensorReading sensorReading = new SensorReading();
      sensorReading.setReadingTime("2019/02/14 04:31:14 +0200");
      sensorReading.setValue(30.0);
      sensorReading.setUom("%");
      sensor.setReading(sensorReading);
      Mockito.when(this.powerIQAPIClient.getSensorById("6566")).thenReturn(sensor);
      Set<String> assetIds = new HashSet<String>();
      assetIds.add("123o89qw4jjasd0");

      List<Asset> assets = new ArrayList<Asset>();
      assets.add(asset);

      List<RealTimeData> realTimeDatas =
            powerIQService.getSensorRealTimeData(createFacility(), assets);
      for (RealTimeData realtimeData : realTimeDatas) {
         TestCase.assertEquals("123o89qw4jjasd0", realtimeData.getAssetID());
         TestCase.assertEquals(1550111474000l, realtimeData.getTime());
         if ("123o89qw4jjasd0".equals(realtimeData.getAssetID())) {
            TestCase.assertEquals("%", realtimeData.getValues().get(0).getUnit());
         }
      }

   }

   @Test
   public void testGetSensorRealTimeData1() {
      HashMap<String, String> justificationfields = generateExtraInfo("6566");
      Asset asset = createAsset();
      asset.setId("123o89qw4jjasd0");
      asset.setJustificationfields(justificationfields);
      Sensor sensor = createSensor();
      sensor.setId(6566);
      sensor.setName("HumiditySensor");
      sensor.setSerialNumber("8999");
      sensor.setType("HumiditySensor");
      Mockito.when(this.powerIQAPIClient.getSensorById("6566")).thenReturn(sensor);

      List<Asset> assets = new ArrayList<Asset>();
      assets.add(asset);

      List<RealTimeData> realTimeDatas =
            powerIQService.getSensorRealTimeData(createFacility(), assets);
      TestCase.assertEquals(0, realTimeDatas.size());

   }

   @Test
   public void testGetSensorRealTimeData2() {
      HashMap<String, String> justificationfields = generateExtraInfo("6566");
      Asset asset = createAsset();
      asset.setId("123o89qw4jjasd0");
      asset.setJustificationfields(justificationfields);

      HashMap<String, String> justificationfields1 = generateExtraInfo("6567");
      Asset asset1 = createAsset();
      asset1.setId("123o89qw4jjasd1");
      asset1.setJustificationfields(justificationfields1);

      Sensor sensor = createSensor();
      SensorReading sensorReading = createReading();
      sensorReading.setUom("%");
      sensor.setId(6566);
      sensor.setName("HumiditySensor");
      sensor.setSerialNumber("8999");
      sensor.setType("HumiditySensor");
      sensor.setReading(sensorReading);
      Mockito.when(this.powerIQAPIClient.getSensorById("6566")).thenReturn(sensor);

      Sensor sensor1 = createSensor();
      SensorReading sensorReading1 = createReading();
      sensorReading1.setUom("F");
      sensor1.setId(6567);
      sensor1.setName("TemperatureSensor");
      sensor1.setSerialNumber("9000");
      sensor1.setType("TemperatureSensor");
      sensor1.setReading(sensorReading1);
      Mockito.when(this.powerIQAPIClient.getSensorById("6567")).thenReturn(sensor1);

      Set<String> assetIds = new HashSet<String>();
      assetIds.add("123o89qw4jjasd0");
      assetIds.add("123o89qw4jjasd1");

      List<Asset> assets = new ArrayList<Asset>();
      assets.add(asset);
      assets.add(asset1);

      List<RealTimeData> realTimeDatas =
            powerIQService.getSensorRealTimeData(createFacility(), assets);
      for(RealTimeData realtimedata:realTimeDatas) {
    	  if("123o89qw4jjasd0".equals(realtimedata.getAssetID())) {
    		  TestCase.assertEquals((double)100, realtimedata.getValues().get(0).getValueNum());
    	  }else {
    		  TestCase.assertEquals((double)(100-32)*5/9, realtimedata.getValues().get(0).getValueNum());
    	  }
      }
   }

   @Test
   public void getLongTime() {
      long time =
            WormholeDateFormat.getLongTime("2019/02/14 04:31:14 +0200", "yyyy/MM/dd HH:mm:ss Z");
      TestCase.assertEquals(1550111474000l, time);
   }

   @Test
   public void testAggregatorSensorIdAndSourceForPdu() {
      Asset pdu = createAsset1();
      Sensor sensor = new Sensor();
      sensor.setId(509);
      sensor.setType(PowerIQService.HumiditySensor);
      String source = "l9i8728d55368540fcba1692";
      pdu = powerIQService.aggregatorSensorIdAndSourceForPdu(pdu, sensor, source);
      TestCase.assertEquals(sensor.getId()+FlowgateConstant.SEPARATOR+source,
            pdu.getJustificationfields().get(AssetSubCategory.Humidity.toString()));
   }

   @Test
   public void testAggregatorSensorIdAndSourceForPdu1() {
      Asset pdu = createAsset1();
      HashMap<String, String> justificationfields = new HashMap<String,String>();
      justificationfields.put(AssetSubCategory.Humidity.toString(), "509"+FlowgateConstant.SEPARATOR+"l9i8728d55368540fcba1692");
      pdu.setJustificationfields(justificationfields);
      Sensor sensor = new Sensor();
      sensor.setId(509);
      sensor.setType(PowerIQService.HumiditySensor);
      String source = "l9i8728d55368540fcba1692";
      pdu = powerIQService.aggregatorSensorIdAndSourceForPdu(pdu, sensor, source);
      TestCase.assertEquals(sensor.getId()+FlowgateConstant.SEPARATOR+source,
            pdu.getJustificationfields().get(AssetSubCategory.Humidity.toString()));
   }

   @Test
   public void testAggregatorSensorIdAndSourceForPdu2() {
      Asset pdu = createAsset1();
      HashMap<String, String> justificationfields = new HashMap<String,String>();
      String filed = "509"+FlowgateConstant.SEPARATOR+"l9i8728d55368540fcba1692,606"+FlowgateConstant.SEPARATOR+"l9i8728d55368540fcba1692";
      justificationfields.put(AssetSubCategory.Humidity.toString(), filed);
      pdu.setJustificationfields(justificationfields);
      Sensor sensor = new Sensor();
      sensor.setId(610);
      sensor.setType(PowerIQService.HumiditySensor);
      String source = "l9i8728d55368540fcba1692";
      pdu = powerIQService.aggregatorSensorIdAndSourceForPdu(pdu, sensor, source);
      TestCase.assertEquals(filed+FlowgateConstant.SPILIT_FLAG+sensor.getId()+FlowgateConstant.SEPARATOR+source,
            pdu.getJustificationfields().get(AssetSubCategory.Humidity.toString()));
   }

   @Test
   public void testAggregatorSensorIdAndSourceForPdu3() {
      Asset pdu = createAsset1();
      HashMap<String, String> justificationfields = new HashMap<String,String>();
      justificationfields.put(AssetSubCategory.Temperature.toString(), "509_l9i8728d55368540fcba1692");
      pdu.setJustificationfields(justificationfields);
      Sensor sensor = new Sensor();
      sensor.setId(606);
      sensor.setType(PowerIQService.HumiditySensor);
      String source = "l9i8728d55368540fcba1692";
      pdu = powerIQService.aggregatorSensorIdAndSourceForPdu(pdu, sensor, source);
      TestCase.assertEquals(sensor.getId()+FlowgateConstant.SEPARATOR+source,
            pdu.getJustificationfields().get(AssetSubCategory.Humidity.toString()));
   }

   HashMap<AdvanceSettingType, String> createAdvanceSettingMap() {
      HashMap<AdvanceSettingType, String> advanceSettingMap =
            new HashMap<AdvanceSettingType, String>();
      advanceSettingMap.put(AdvanceSettingType.DateFormat, "yyyy/MM/dd HH:mm:ss Z");
      advanceSettingMap.put(AdvanceSettingType.TimeZone, "GMT");
      advanceSettingMap.put(AdvanceSettingType.HUMIDITY_UNIT, "%");
      advanceSettingMap.put(AdvanceSettingType.TEMPERATURE_UNIT, "C");
      advanceSettingMap.put(AdvanceSettingType.PDU_AMPS_UNIT, "A");
      advanceSettingMap.put(AdvanceSettingType.PDU_POWER_UNIT, "KW");
      advanceSettingMap.put(AdvanceSettingType.PDU_VOLT_UNIT, "V");
      return advanceSettingMap;
   }

   FacilitySoftwareConfig createFacility() {
      FacilitySoftwareConfig config = new FacilitySoftwareConfig();
      config.setAdvanceSetting(createAdvanceSettingMap());
      return config;
   }

   List<Pdu> getPdus() {
      List<Pdu> pdus = new ArrayList<Pdu>();
      Pdu pdu1 = createPdu();
      pdu1.setId(1);
      pdu1.setName("pdu-1");
      Pdu pdu2 = createPdu();
      pdu2.setId(2);
      pdu2.setName("pek-wor-pdu-02");
      pdus.add(pdu1);
      pdus.add(pdu2);
      return pdus;
   }

   List<DataCenter> getDataCenters() {
      List<DataCenter> dataCenters = new ArrayList<DataCenter>();
      DataCenter dataCenter = new DataCenter();
      dataCenter.setName("DataCenter");
      dataCenter.setId(7);
      dataCenter.setCity("Yerevan");
      dataCenter.setCountry("Armenia");
      dataCenters.add(dataCenter);
      return dataCenters;
   }

   List<Floor> getFloors() {
      List<Floor> floors = new ArrayList<Floor>();
      Floor floor = new Floor();
      floor.setName("floor01");
      floor.setId(5);
      Parent parent = new Parent();
      parent.setId(7);
      parent.setType("data_center");
      floor.setParent(parent);
      floors.add(floor);
      return floors;
   }

   List<Room> getRooms() {
      List<Room> rooms = new ArrayList<Room>();
      Room room = new Room();
      room.setName("room01");
      room.setId(8);
      Parent parent = new Parent();
      parent.setId(5);
      parent.setType("floor");
      room.setParent(parent);
      rooms.add(room);
      return rooms;
   }

   List<Aisle> getAisles() {
      List<Aisle> aisles = new ArrayList<Aisle>();
      Aisle aisle = new Aisle();
      aisle.setName("aisle");
      aisle.setId(2);
      Parent parent = new Parent();
      parent.setId(8);
      parent.setType("room");
      aisle.setParent(parent);
      aisles.add(aisle);
      return aisles;
   }

   List<Rack> getRacks() {
      List<Rack> racks = new ArrayList<Rack>();
      Rack rack = new Rack();
      rack.setName("rack");
      rack.setId(2);
      Parent parent = new Parent();
      parent.setId(1);
      parent.setType("row");
      rack.setParent(parent);
      racks.add(rack);
      return racks;
   }

   List<Row> getRows() {
      List<Row> rows = new ArrayList<Row>();
      Row row = new Row();
      row.setName("2");
      row.setId(1);
      Parent parent = new Parent();
      parent.setId(2);
      parent.setType("aisle");
      row.setParent(parent);
      rows.add(row);
      return rows;
   }

   List<Sensor> getSensors() {
      List<Sensor> sensors = new ArrayList<Sensor>();
      Sensor sensor = createSensor();
      sensor.setId(6566);
      sensor.setName("HumiditySensor");
      sensor.setSerialNumber("8999");
      sensor.setType("HumiditySensor");
      sensors.add(sensor);

      Sensor sensor1 = createSensor();
      sensor1.setId(7878);
      sensor1.setSerialNumber("5487");
      sensor1.setName("TemperatureSensor01");
      sensor1.setType("TemperatureSensor");
      sensor1.setPduId(2L);
      sensors.add(sensor1);
      return sensors;
   }

   List<Asset> createAssets() {
      List<Asset> assets = new ArrayList<Asset>();
      assets.add(createAsset());
      return assets;
   }

   Pdu createPdu() {
      Pdu pdu = new Pdu();
      pdu.setName("pek-wor-pdu-02");
      return pdu;
   }

   Sensor createSensor() {
      Sensor sensor = new Sensor();
      Parent parent = new Parent();
      parent.setId(2);
      parent.setType("rack");
      sensor.setParent(parent);
      return sensor;
   }

   SensorReading createReading() {
       SensorReading sensorReading = new SensorReading();
       sensorReading.setMaxValue(1.0);
       sensorReading.setMinValue(0d);
       sensorReading.setReadingTime("2019/02/14 04:31:14 +0200");
       sensorReading.setValue(100.0);
       return sensorReading;
   }
   Asset createAsset() {
      Asset asset = new Asset();
      asset.setAssetName("pek-wor-sensor-02");
      asset.setAssetNumber(12345);
      asset.setAssetSource("l9i8728d55368540fcba1692");
      asset.setCategory(AssetCategory.Sensors);
      HashMap<String, String> justificationfields = generateExtraInfo("106");
      asset.setJustificationfields(justificationfields);
      asset.setModel("Dell 750");
      asset.setManufacturer("Dell");
      return asset;
   }

   Asset createAsset1() {
      Asset asset = new Asset();
      asset.setAssetName("pek-wor-pdu-02");
      asset.setAssetNumber(89765);
      asset.setAssetSource("po09imkhdplbvf540fwusy67n");
      asset.setCategory(AssetCategory.PDU);
      asset.setModel("Dell 750");
      asset.setManufacturer("Dell");
      return asset;
   }

   public List<Asset> getAssets(AssetCategory category) {
      List<Asset> assets = new ArrayList<Asset>();
      switch (category) {
      case Sensors:
         assets.add(createAsset());
         break;
      case PDU:
         Asset pdu = createAsset1();
         pdu.setFloor("1st");
         pdu.setRoom("SJC31");
         pdu.setBuilding("2805 Lafayette St");
         pdu.setCity("Santa Clara");
         pdu.setCountry("USA");
         pdu.setRegion("NASA");
         assets.add(pdu);
         break;
      default:
         break;
      }
      return assets;
   }

   public ResponseEntity<FacilitySoftwareConfig[]> getFacilitySoftwareByType(
         SoftwareType category) {
      FacilitySoftwareConfig[] configs = new FacilitySoftwareConfig[1];
      configs[0] = new FacilitySoftwareConfig();
      switch (category) {
      case PowerIQ:
         configs[0].setId("l9i8728d55368540fcba1692");
         break;
      case Nlyte:
         configs[0].setId("po09imkhdplbvf540fwusy67n");
      default:
         break;
      }
      configs[0].setType(category);
      return new ResponseEntity<FacilitySoftwareConfig[]>(configs, HttpStatus.OK);
   }

   public HashMap<String,String> generateExtraInfo(String sensorId){
      HashMap<String,String> sensorInfoMap = new HashMap<String,String>();
      sensorInfoMap.put(FlowgateConstant.SENSOR_ID_FROM_POWERIQ, sensorId);
      String sensorInfo = null;
      ObjectMapper mapper = new ObjectMapper();
      try {
         sensorInfo = mapper.writeValueAsString(sensorInfoMap);
      } catch (JsonProcessingException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      HashMap<String,String> extraInfo = new HashMap<String,String>();
      extraInfo.put(FlowgateConstant.SENSOR, sensorInfo);
      return extraInfo;
   }
}
