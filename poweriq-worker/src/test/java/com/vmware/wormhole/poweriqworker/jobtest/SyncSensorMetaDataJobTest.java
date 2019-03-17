/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.poweriqworker.jobtest;

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
import com.vmware.wormhole.common.WormholeConstant;
import com.vmware.wormhole.common.model.Asset;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig;
import com.vmware.wormhole.common.model.RealTimeData;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.wormhole.common.utils.WormholeDateFormat;
import com.vmware.wormhole.poweriqworker.client.PowerIQAPIClient;
import com.vmware.wormhole.poweriqworker.jobs.PowerIQService;
import com.vmware.wormhole.poweriqworker.model.Aisle;
import com.vmware.wormhole.poweriqworker.model.DataCenter;
import com.vmware.wormhole.poweriqworker.model.Floor;
import com.vmware.wormhole.poweriqworker.model.Parent;
import com.vmware.wormhole.poweriqworker.model.Pdu;
import com.vmware.wormhole.poweriqworker.model.Rack;
import com.vmware.wormhole.poweriqworker.model.Room;
import com.vmware.wormhole.poweriqworker.model.Row;
import com.vmware.wormhole.poweriqworker.model.Sensor;
import com.vmware.wormhole.poweriqworker.model.SensorReading;

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
   private PowerIQService powerIQService;

   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
      Mockito.when(this.wormholeAPIClient.getFacilitySoftwareByType(SoftwareType.PowerIQ))
            .thenReturn(getFacilitySoftwareByType(SoftwareType.PowerIQ));
      Mockito.when(this.wormholeAPIClient.getAssetsBySourceAndType("l9i8728d55368540fcba1692",
            AssetCategory.Sensors)).thenReturn(getAssets(AssetCategory.Sensors));

   }

   @Test
   public void testGetAssetsFromWormhole() {
      Map<String, Asset> assetMap =
            powerIQService.getAssetsFromWormhole("l9i8728d55368540fcba1692");
      TestCase.assertEquals(true,
            assetMap.containsKey("106"));
   }

   @Test
   public void testFilterAsset() {
      List<Asset> asset = powerIQService.filterAsset(null, null);
      TestCase.assertEquals(null, asset);
   }

   @Test
   public void testFilterAsset1() {
      Map<String, Asset> exsitingAsset = new HashMap<String, Asset>();
      List<Asset> asset = powerIQService.filterAsset(exsitingAsset, null);
      TestCase.assertEquals(null, asset);
   }

   @Test
   public void testFilterAsset2() {
      List<Asset> assetsFromPowerIQ = createAssets();
      List<Asset> asset = powerIQService.filterAsset(null, assetsFromPowerIQ);
      TestCase.assertEquals(12345, asset.get(0).getAssetNumber());
   }
   
   @Test
   public void testFilterAsset3() {
      Map<String, Asset> assetMap =
            powerIQService.getAssetsFromWormhole("l9i8728d55368540fcba1692");
      List<Asset> assetsFromPowerIQ = createAssets();
      assetsFromPowerIQ.get(0).setAssetName("Temp 1");
      List<Asset> asset = powerIQService.filterAsset(assetMap, assetsFromPowerIQ);
      TestCase.assertEquals(12345, asset.get(0).getAssetNumber());
      TestCase.assertEquals("Temp 1", asset.get(0).getAssetName());
   }
   
   @Test
   public void testFilterAsset4() {
      Map<String, Asset> assetMap =
            powerIQService.getAssetsFromWormhole("l9i8728d55368540fcba1692");
      List<Asset> assetsFromPowerIQ = createAssets();
      assetsFromPowerIQ.get(0).setJustificationfields(new HashMap());
      List<Asset> asset = powerIQService.filterAsset(assetMap, assetsFromPowerIQ);
      TestCase.assertEquals(0, asset.size());
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
      Map<Integer, Rack> racks = powerIQService.getRacksMap(powerIQAPIClient);
      TestCase.assertEquals(true, racks.containsKey(2));
   }

   @Test
   public void testGetRowsMap() {
      Mockito.when(this.powerIQAPIClient.getRows()).thenReturn(getRows());
      Map<Integer, Row> rows = powerIQService.getRowsMap(powerIQAPIClient);
      TestCase.assertEquals(true, rows.containsKey(1));
   }

   @Test
   public void testGetAislesMap() {
      Mockito.when(this.powerIQAPIClient.getAisles()).thenReturn(getAisles());
      Map<Integer, Aisle> aisles = powerIQService.getAislesMap(powerIQAPIClient);
      TestCase.assertEquals(true, aisles.containsKey(2));
   }

   @Test
   public void testGetRoomsMap() {
      Mockito.when(this.powerIQAPIClient.getRooms()).thenReturn(getRooms());
      Map<Integer, Room> rooms = powerIQService.getRoomsMap(powerIQAPIClient);
      TestCase.assertEquals(true, rooms.containsKey(8));
   }

   @Test
   public void testGetFloorsMap() {
      Mockito.when(this.powerIQAPIClient.getFloors()).thenReturn(getFloors());
      Map<Integer, Floor> floors = powerIQService.getFloorsMap(powerIQAPIClient);
      TestCase.assertEquals(true, floors.containsKey(5));
   }

   @Test
   public void testGetDataCentersMap() {
      Mockito.when(this.powerIQAPIClient.getDataCenters()).thenReturn(getDataCenters());
      Map<Integer, DataCenter> dataCenters = powerIQService.getDataCentersMap(powerIQAPIClient);
      TestCase.assertEquals(true, dataCenters.containsKey(7));
   }

   @Test
   public void testGetPduMap() {
      Mockito.when(this.powerIQAPIClient.getPdus()).thenReturn(getPdus());
      Map<Integer, Pdu> pdus = powerIQService.getPduMap(powerIQAPIClient);
      TestCase.assertEquals(true, pdus.containsKey(1));
      TestCase.assertEquals(true, pdus.containsKey(2));
   }

   @Test
   public void testGetAssetMap() {
      Mockito.when(this.wormholeAPIClient.getFacilitySoftwareByType(SoftwareType.Nlyte))
            .thenReturn(getFacilitySoftwareByType(SoftwareType.Nlyte));
      Mockito.when(this.wormholeAPIClient.getAssetsBySourceAndType("po09imkhdplbvf540fwusy67n",
            AssetCategory.PDU)).thenReturn(getAssets(AssetCategory.PDU));
      Map<String, Asset> assetsMap = powerIQService.getPDUAssetMap();
      TestCase.assertEquals(true, assetsMap.containsKey("pek-wor-pdu-02"));
   }

   @Test
   public void testGetSensorMetaData() {
      Mockito.when(this.powerIQAPIClient.getSensors()).thenReturn(new ArrayList<Sensor>());
      Mockito.when(this.powerIQAPIClient.getPdus()).thenReturn(getPdus());
      List<Asset> assets = powerIQService.getSensorMetaData(powerIQAPIClient, "1");
      TestCase.assertEquals(true, assets.isEmpty());
   }

   @Test
   public void testGetSensorMetaData1() {
      Mockito.when(this.powerIQAPIClient.getSensors()).thenReturn(getSensors());
      Mockito.when(this.powerIQAPIClient.getPdus()).thenReturn(new ArrayList<Pdu>());
      Mockito.when(this.powerIQAPIClient.getRacks()).thenReturn(getRacks());
      Mockito.when(this.powerIQAPIClient.getRows()).thenReturn(getRows());
      Mockito.when(this.powerIQAPIClient.getAisles()).thenReturn(getAisles());
      Mockito.when(this.powerIQAPIClient.getRooms()).thenReturn(getRooms());
      Mockito.when(this.powerIQAPIClient.getFloors()).thenReturn(getFloors());
      Mockito.when(this.powerIQAPIClient.getDataCenters()).thenReturn(getDataCenters());
      Mockito.when(this.wormholeAPIClient.getFacilitySoftwareByType(SoftwareType.Nlyte))
            .thenReturn(getFacilitySoftwareByType(SoftwareType.Nlyte));
      Mockito.when(this.wormholeAPIClient.getAssetsBySourceAndType("po09imkhdplbvf540fwusy67n",
            AssetCategory.PDU)).thenReturn(getAssets(AssetCategory.PDU));

      Map<String, Asset> assetsMap = new HashMap<String, Asset>();
      Asset pdu = createAsset1();
      assetsMap.put(pdu.getAssetName(), pdu);
      List<Asset> assets = powerIQService.getSensorMetaData(powerIQAPIClient, "po09imkhdplbvf540fwusy67n");
      TestCase.assertEquals(2, assets.size());
      for (Asset asset : assets) {
         if ("8999".equals(asset.getSerialnumber())) {
            TestCase.assertEquals("HumiditySensor", asset.getAssetName());
            TestCase.assertEquals("Yerevan", asset.getCity());
            TestCase.assertEquals("Armenia", asset.getCountry());
         } else {
            TestCase.assertEquals("TemperatureSensor01", asset.getAssetName());
            TestCase.assertEquals("5487", asset.getSerialnumber());
            TestCase.assertEquals("Yerevan", asset.getCity());
            TestCase.assertEquals("Armenia", asset.getCountry());
         }
      }
   }

   @Test
   public void testGetSensorMetaData2() {
      Mockito.when(this.powerIQAPIClient.getSensors()).thenReturn(getSensors());
      Mockito.when(this.powerIQAPIClient.getPdus()).thenReturn(getPdus());
      Mockito.when(this.powerIQAPIClient.getRacks()).thenReturn(getRacks());
      Mockito.when(this.powerIQAPIClient.getRows()).thenReturn(getRows());
      Mockito.when(this.powerIQAPIClient.getAisles()).thenReturn(getAisles());
      Mockito.when(this.powerIQAPIClient.getRooms()).thenReturn(getRooms());
      Mockito.when(this.powerIQAPIClient.getFloors()).thenReturn(getFloors());
      Mockito.when(this.powerIQAPIClient.getDataCenters()).thenReturn(getDataCenters());
      Mockito.when(this.wormholeAPIClient.getFacilitySoftwareByType(SoftwareType.Nlyte))
            .thenReturn(getFacilitySoftwareByType(SoftwareType.Nlyte));
      Mockito.when(this.wormholeAPIClient.getAssetsBySourceAndType("po09imkhdplbvf540fwusy67n",
            AssetCategory.PDU)).thenReturn(getPDUAssets(AssetCategory.PDU));
      List<Asset> assets = powerIQService.getSensorMetaData(powerIQAPIClient, "po09imkhdplbvf540fwusy67n");
      TestCase.assertEquals(3, assets.size());
      for (Asset asset : assets) {
         if ("8999".equals(asset.getSerialnumber())) {
            TestCase.assertEquals("HumiditySensor", asset.getAssetName());
            TestCase.assertEquals("Yerevan", asset.getCity());
            TestCase.assertEquals("Armenia", asset.getCountry());
         }else if("pek-wor-pdu-02".equals(asset.getAssetName())) {
            TestCase.assertEquals(AssetCategory.PDU, asset.getCategory());
            String sensorIdAndSource = asset.getJustificationfields().get(AssetSubCategory.Temperature.toString());
            TestCase.assertEquals("7878_po09imkhdplbvf540fwusy67n", sensorIdAndSource);
         }
         else {
            TestCase.assertEquals("TemperatureSensor01", asset.getAssetName());
            TestCase.assertEquals("5487", asset.getSerialnumber());
            TestCase.assertEquals("Santa Clara", asset.getCity());
            TestCase.assertEquals("USA", asset.getCountry());
         }
      }
   }

   @Test
   public void testGetSensorRealTimeData() {
      HashMap<String, String> justificationfields = new HashMap<String, String>();
      justificationfields.put("Sensor_ID", "6566");
      Asset asset = createAsset();
      asset.setId("123o89qw4jjasd0");
      asset.setJustificationfields(justificationfields);
      Mockito.when(this.wormholeAPIClient.getAssetByID("123o89qw4jjasd0"))
            .thenReturn(new ResponseEntity<Asset>(asset, HttpStatus.OK));
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
   
      List<RealTimeData> realTimeDatas =
            powerIQService.getSensorRealTimeData(powerIQAPIClient, createAdvanceSettingMap(), assetIds);
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
      HashMap<String, String> justificationfields = new HashMap<String, String>();
      justificationfields.put("Sensor_ID", "6566");
      Asset asset = createAsset();
      asset.setId("123o89qw4jjasd0");
      asset.setJustificationfields(justificationfields);
      Mockito.when(this.wormholeAPIClient.getAssetByID("123o89qw4jjasd0"))
            .thenReturn(new ResponseEntity<Asset>(asset, HttpStatus.OK));
      Sensor sensor = createSensor();
      sensor.setId(6566);
      sensor.setName("HumiditySensor");
      sensor.setSerialNumber("8999");
      sensor.setType("HumiditySensor");
      Mockito.when(this.powerIQAPIClient.getSensorById("6566")).thenReturn(sensor);
      Set<String> assetIds = new HashSet<String>();
      assetIds.add("123o89qw4jjasd0");
      List<RealTimeData> realTimeDatas =
            powerIQService.getSensorRealTimeData(powerIQAPIClient, createAdvanceSettingMap(), assetIds);
      TestCase.assertEquals(0, realTimeDatas.size());

   }
   
   @Test
   public void testGetSensorRealTimeData2() {
      HashMap<String, String> justificationfields = new HashMap<String, String>();
      justificationfields.put("Sensor_ID", "6566");
      Asset asset = createAsset();
      asset.setId("123o89qw4jjasd0");
      asset.setJustificationfields(justificationfields);
      Mockito.when(this.wormholeAPIClient.getAssetByID("123o89qw4jjasd0"))
      .thenReturn(new ResponseEntity<Asset>(asset, HttpStatus.OK));
      
      HashMap<String, String> justificationfields1 = new HashMap<String, String>();
      justificationfields1.put("Sensor_ID", "6567");
      Asset asset1 = createAsset();
      asset1.setId("123o89qw4jjasd1");
      asset1.setJustificationfields(justificationfields1);
      Mockito.when(this.wormholeAPIClient.getAssetByID("123o89qw4jjasd1"))
      .thenReturn(new ResponseEntity<Asset>(asset1, HttpStatus.OK));
      
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
      List<RealTimeData> realTimeDatas =
            powerIQService.getSensorRealTimeData(powerIQAPIClient, createAdvanceSettingMap(), assetIds);
      TestCase.assertEquals((double)(100-32)*5/9, realTimeDatas.get(0).getValues().get(0).getValueNum());
      TestCase.assertEquals((double)100, realTimeDatas.get(1).getValues().get(0).getValueNum());

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
      TestCase.assertEquals(sensor.getId()+WormholeConstant.SENSOR_SOURCE_SPLIT_FLAG+source,
            pdu.getJustificationfields().get(AssetSubCategory.Humidity.toString()));
   }

   @Test
   public void testAggregatorSensorIdAndSourceForPdu1() {
      Asset pdu = createAsset1();
      HashMap<String, String> justificationfields = new HashMap<String,String>();
      justificationfields.put(AssetSubCategory.Humidity.toString(), "509_l9i8728d55368540fcba1692");
      pdu.setJustificationfields(justificationfields);
      Sensor sensor = new Sensor();
      sensor.setId(509);
      sensor.setType(PowerIQService.HumiditySensor);
      String source = "l9i8728d55368540fcba1692";
      pdu = powerIQService.aggregatorSensorIdAndSourceForPdu(pdu, sensor, source);
      TestCase.assertEquals(sensor.getId()+WormholeConstant.SENSOR_SOURCE_SPLIT_FLAG+source,
            pdu.getJustificationfields().get(AssetSubCategory.Humidity.toString()));
   }
   
   @Test
   public void testAggregatorSensorIdAndSourceForPdu2() {
      Asset pdu = createAsset1();
      HashMap<String, String> justificationfields = new HashMap<String,String>();
      justificationfields.put(AssetSubCategory.Humidity.toString(), "509_l9i8728d55368540fcba1692,606_l9i8728d55368540fcba1692");
      pdu.setJustificationfields(justificationfields);
      Sensor sensor = new Sensor();
      sensor.setId(610);
      sensor.setType(PowerIQService.HumiditySensor);
      String source = "l9i8728d55368540fcba1692";
      pdu = powerIQService.aggregatorSensorIdAndSourceForPdu(pdu, sensor, source);
      TestCase.assertEquals("509_l9i8728d55368540fcba1692,606_l9i8728d55368540fcba1692"+WormholeConstant.SENSOR_SPILIT_FLAG+sensor.getId()+WormholeConstant.SENSOR_SOURCE_SPLIT_FLAG+source,
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
      TestCase.assertEquals(sensor.getId()+WormholeConstant.SENSOR_SOURCE_SPLIT_FLAG+source,
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
      sensor1.setPduId(2);
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
       sensorReading.setMaxValue("1");
       sensorReading.setMinValue("0");
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
      HashMap<String, String> justificationfields = new HashMap<String, String>();
      justificationfields.put("Sensor_ID", "106");
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

      Asset[] assets = new Asset[1];
      public ResponseEntity<Asset[]> getAssets(AssetCategory category) {
      switch (category) {
      case Sensors:
         assets[0] = createAsset();
         break;
      case PDU:
         assets[0] = createAsset1();
         break;
      default:
         break;
      }
      return new ResponseEntity<Asset[]>(assets, HttpStatus.OK);
   }

   public ResponseEntity<Asset[]> getPDUAssets(AssetCategory category) {
      Asset[] assets = new Asset[1];
      switch (category) {
      case Sensors:
         assets[0] = createAsset();
         break;
      case PDU:
         Asset pdu = createAsset1();
         pdu.setFloor("1st");
         pdu.setRoom("SJC31");
         pdu.setBuilding("2805 Lafayette St");
         pdu.setCity("Santa Clara");
         pdu.setCountry("USA");
         pdu.setRegion("NASA");
         assets[0] = pdu;
         break;
      default:
         break;
      }
      return new ResponseEntity<Asset[]>(assets, HttpStatus.OK);
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
}
