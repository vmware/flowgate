package com.vmware.flowgate.poweriqworker.jobtest;

import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vmware.flowgate.poweriqworker.config.ServiceKeyConfig;
import com.vmware.flowgate.poweriqworker.model.Inlet;
import com.vmware.flowgate.poweriqworker.model.InletPoleReading;
import com.vmware.flowgate.poweriqworker.model.InletReading;
import com.vmware.flowgate.poweriqworker.model.LocationInfo;
import com.vmware.flowgate.poweriqworker.model.Outlet;
import com.vmware.flowgate.poweriqworker.model.OutletReading;
import com.vmware.flowgate.poweriqworker.model.Reading;
import com.vmware.flowgate.poweriqworker.redis.TestRedisConfiguration;
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
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.Parent;
import com.vmware.flowgate.poweriqworker.client.PowerIQAPIClient;
import com.vmware.flowgate.poweriqworker.jobs.PowerIQService;
import com.vmware.flowgate.poweriqworker.model.Pdu;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestRedisConfiguration.class)
@ActiveProfiles("test")
public class SyncPduAssetJobTest {

   @Mock
   private WormholeAPIClient wormholeAPIClient;


   @Mock
   private PowerIQAPIClient powerIQAPIClient;

   @Mock
   private ServiceKeyConfig serviceKeyConfig;

   @Spy
   @InjectMocks
   private PowerIQService powerIQService = new PowerIQService();

   private ObjectMapper mapper = new ObjectMapper();


   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
      Mockito.when(this.wormholeAPIClient.getFacilitySoftwareInternalByType(SoftwareType.PowerIQ))
            .thenReturn(new SyncSensorMetaDataJobTest().getFacilitySoftwareByType(SoftwareType.PowerIQ));
      Mockito.when(this.wormholeAPIClient.getAllAssetsBySourceAndType("l9i8728d55368540fcba1692",
            AssetCategory.PDU)).thenReturn(getAssets());
      Mockito.doReturn(powerIQAPIClient).when(powerIQService).createClient(any(FacilitySoftwareConfig.class));
   }

   @Test
   public void testGetPDUIDAndAssetMap() {
      List<Asset> pdusFromFlowgate = getAssets();
      Map<String, Asset> map = powerIQService.getPDUIDAndAssetMap(pdusFromFlowgate);
      TestCase.assertEquals(pdusFromFlowgate.get(0).getAssetName(), map.get("128").getAssetName());
   }

   @Test
   public void testGeneratePduRateInfoMap() {
      Pdu pdu = new Pdu();
      pdu.setRatedAmps("34A");
      pdu.setRatedVa("6.4-7.7kVA");
      pdu.setRatedVolts("220-240V");
      Map<String,String> map = powerIQService.generatePduRateInfoMap(pdu);
      TestCase.assertEquals("34", map.get(FlowgateConstant.PDU_RATE_AMPS));
      TestCase.assertEquals("6.4", map.get(FlowgateConstant.PDU_MIN_RATE_POWER));
      TestCase.assertEquals("7.7", map.get(FlowgateConstant.PDU_MAX_RATE_POWER));
      TestCase.assertEquals("220", map.get(FlowgateConstant.PDU_MIN_RATE_VOLTS));
      TestCase.assertEquals("240", map.get(FlowgateConstant.PDU_MAX_RATE_VOLTS));
   }

   @Test
   public void testGeneratePduRateInfoMap1() {
      Pdu pdu = new Pdu();
      pdu.setRatedAmps("34A");
      pdu.setRatedVa("7.7kVA");
      pdu.setRatedVolts("240V");
      Map<String,String> map = powerIQService.generatePduRateInfoMap(pdu);
      TestCase.assertEquals("34", map.get(FlowgateConstant.PDU_RATE_AMPS));
      TestCase.assertEquals("7.7", map.get(FlowgateConstant.PDU_MIN_RATE_POWER));
      TestCase.assertEquals("7.7", map.get(FlowgateConstant.PDU_MAX_RATE_POWER));
      TestCase.assertEquals("240", map.get(FlowgateConstant.PDU_MIN_RATE_VOLTS));
      TestCase.assertEquals("240", map.get(FlowgateConstant.PDU_MAX_RATE_VOLTS));
   }

   @Test
   public void testGetAssetIdfromformular() throws JsonProcessingException {
      List<Asset> assets = new ArrayList<>();
      Asset asset = createAsset();
      Map<String, String> formulars = new HashMap<>();
      Map<String, Map<String, String>> sensorFormulars = new HashMap<String, Map<String, String>>();
      Map<String, String> humidityLocationAndIdMap = new HashMap<String,String>();
      humidityLocationAndIdMap.put("1", "po09imkhdplbvf540fwusy67n");
      Map<String, String> tempreatureLocationAndIdMap = new HashMap<String,String>();
      tempreatureLocationAndIdMap.put("2", "asdasd2s2gxvf5wfwudwadbn");
      sensorFormulars.put(MetricName.PDU_HUMIDITY, humidityLocationAndIdMap);
      sensorFormulars.put(MetricName.PDU_TEMPERATURE, tempreatureLocationAndIdMap);
      formulars.put(FlowgateConstant.SENSOR, mapper.writeValueAsString(sensorFormulars));
      asset.setMetricsformulars(formulars);
      assets.add(asset);
      Set<String> assetIDs = powerIQService.getAssetIdfromformular(assets);
      TestCase.assertEquals(2, assetIDs.size());
      Map<String,String> map = new HashMap<String,String>();
      map.put("po09imkhdplbvf540fwusy67n", "po09imkhdplbvf540fwusy67n");
      map.put("asdasd2s2gxvf5wfwudwadbn", "asdasd2s2gxvf5wfwudwadbn");
      for(String assetid : assetIDs) {
         TestCase.assertEquals(map.get(assetid), assetid);
      }
   }

   @Test
   public void testUpdatePduMetricformular() throws JsonProcessingException {
      List<Asset> assets = new ArrayList<>();
      Asset asset = createAsset();
      asset.setCabinetUnitPosition(1);
      asset.setId("po09imkhdplbvf540fwusy67n");
      asset.setCategory(AssetCategory.Sensors);
      asset.setSubCategory(AssetSubCategory.Humidity);
      Parent parent = new Parent();
      parent.setParentId("125");
      parent.setType("PDU");
      asset.setParent(parent);
      assets.add(asset);

      Asset temp = createAsset();
      temp.setCabinetUnitPosition(2);
      temp.setId("qwerxfbsd75sda23plbswgfwusyasn");
      temp.setCategory(AssetCategory.Sensors);
      temp.setSubCategory(AssetSubCategory.Temperature);
      Parent parent1 = new Parent();
      parent1.setParentId("125");
      parent1.setType("PDU");
      temp.setParent(parent);
      assets.add(temp);

      Map<String,Asset> pduIdAndAssetMap = new HashMap<String,Asset>();
      Asset pduAsset = createAsset();
      pduIdAndAssetMap.put("125", pduAsset);

      Set<Asset> pduNeedTosave = powerIQService.updatePduMetricformular(assets, pduIdAndAssetMap);
      TestCase.assertEquals(1, pduNeedTosave.size());
      Asset pdu = pduNeedTosave.iterator().next();
      TestCase.assertEquals(pduAsset.getAssetName(), pdu.getAssetName());
      try {
         Map<String, String> formulars = pdu.getMetricsformulars();
         Map<String, Map<String, String>> sensorFormulars = mapper.readValue(formulars.get(FlowgateConstant.SENSOR), new TypeReference<Map<String, Map<String, String>>>() {});
         Map<String, String> humidityLocationAndIdMap = sensorFormulars.get(MetricName.PDU_HUMIDITY);
         TestCase.assertEquals("po09imkhdplbvf540fwusy67n", humidityLocationAndIdMap.get(FlowgateConstant.RACK_UNIT_PREFIX + "1"));
         Map<String, String> tempLocationAndIdMap = sensorFormulars.get(MetricName.PDU_TEMPERATURE);
         TestCase.assertEquals("qwerxfbsd75sda23plbswgfwusyasn", tempLocationAndIdMap.get(FlowgateConstant.RACK_UNIT_PREFIX + "2"));
      }catch (Exception e) {
         TestCase.fail();
      }
   }

   @Test
   public void testUpdatePduMetricformular2() throws JsonProcessingException {
      List<Asset> assets = new ArrayList<>();
      Asset asset = createAsset();
      asset.setCabinetUnitPosition(1);
      asset.setId("po09imkhdplbvf540fwusy67n");
      asset.setCategory(AssetCategory.Sensors);
      asset.setSubCategory(AssetSubCategory.Humidity);
      Map<String,String> sensorInfoMap = new HashMap<String,String>();
      sensorInfoMap.put(FlowgateConstant.POSITION, "INLET");
      ObjectMapper mapper = new ObjectMapper();
      HashMap<String,String> justfication = new HashMap<String,String>();
      try {
         justfication.put(FlowgateConstant.SENSOR, mapper.writeValueAsString(sensorInfoMap));
      } catch (JsonProcessingException e1) {
         TestCase.fail();
      }
      asset.setJustificationfields(justfication);
      Parent parent = new Parent();
      parent.setParentId("125");
      parent.setType("PDU");
      asset.setParent(parent);
      assets.add(asset);
      Map<String,Asset> pduIdAndAssetMap = new HashMap<String,Asset>();
      Asset pduAsset = createAsset();
      pduIdAndAssetMap.put("125", pduAsset);

      Set<Asset> pduNeedTosave = powerIQService.updatePduMetricformular(assets, pduIdAndAssetMap);
      TestCase.assertEquals(1, pduNeedTosave.size());
      Asset pdu = pduNeedTosave.iterator().next();
      TestCase.assertEquals(pduAsset.getAssetName(), pdu.getAssetName());
      try {
         Map<String, String> formulars = pdu.getMetricsformulars();
         Map<String, Map<String, String>> sensorFormulars = mapper.readValue(formulars.get(FlowgateConstant.SENSOR), new TypeReference<Map<String, Map<String, String>>>() {});
         Map<String, String> humidityLocationAndIdMap = sensorFormulars.get(MetricName.PDU_HUMIDITY);
         TestCase.assertEquals("po09imkhdplbvf540fwusy67n", humidityLocationAndIdMap.get(FlowgateConstant.RACK_UNIT_PREFIX + "1"+FlowgateConstant.SEPARATOR+sensorInfoMap.get(FlowgateConstant.POSITION)));
      }catch (Exception e) {
         TestCase.fail();
      }
   }

   List<Asset> getAssets() {
      List<Asset> assets = new ArrayList<Asset>();
      Asset pdu = createAsset();
      pdu.setFloor("1st");
      pdu.setRoom("SJC31");
      pdu.setBuilding("2805 Lafayette St");
      pdu.setCity("Santa Clara");
      pdu.setCountry("USA");
      pdu.setRegion("NASA");
      assets.add(pdu);
      return assets;
   }

   Asset createAsset() {
      Asset asset = new Asset();
      ObjectMapper mapper = new ObjectMapper();
      asset.setAssetName("pek-wor-pdu-02");
      asset.setAssetNumber(89765);
      asset.setAssetSource("po09imkhdplbvf540fwusy67n");
      asset.setCategory(AssetCategory.PDU);
      asset.setModel("Dell 750");
      asset.setManufacturer("Dell");
      Map<String,String> pduInfo = new HashMap<String,String>();
      pduInfo.put(FlowgateConstant.PDU_ID_FROM_POWERIQ, "128");
      String pduInfoString = null;
      try {
         pduInfoString = mapper.writeValueAsString(pduInfo);
      } catch (JsonProcessingException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      HashMap<String,String> justficationfields = new HashMap<String,String>();
      justficationfields.put(FlowgateConstant.PDU, pduInfoString);
      asset.setJustificationfields(justficationfields);
      return asset;
   }

   @Test
   public void testSavePduAssetsToFlowgate() {
      Map<String, Asset> existedPduAssets = new HashMap<>();
      Asset asset1 = createAsset();
      asset1.setId("126134615724572");
      Asset asset2 = createAsset();
      asset2.setId("BOQBNBHQOQJAOJQY");
      existedPduAssets.put(asset1.getId(), asset1);
      existedPduAssets.put(asset2.getId(), asset2);
      String assetSource = "UGVINQVNQIGQGQIDNKD";
      LocationInfo location = null;

      Pdu pdu1 = createPdu();
      pdu1.setId(126134615724572L);
      pdu1.setName("pek-wor-pdu-01");
      Pdu pdu2 = createPdu();
      pdu2.setId(226134615724572L);
      pdu2.setName("pek-wor-pdu-02");

      Mockito.doReturn(new ArrayList<>(Arrays.asList(pdu1, pdu2))).when(powerIQAPIClient).getPdus(100,0);
      Mockito.doReturn(null).when(powerIQAPIClient).getPdus(100,100);
      Mockito.doReturn(getOutlets()).when(powerIQAPIClient).getOutlets(Mockito.anyLong());
      Mockito.doReturn(getInlets()).when(powerIQAPIClient).getInlets(Mockito.anyLong());
      Mockito.doReturn(new ResponseEntity<Void>(HttpStatus.OK)).when(wormholeAPIClient).saveAssets(Mockito.any(Asset.class));
      Mockito.doReturn("23551d6dacf2432c8a3edbc6bbc922cd").when(powerIQService).getAssetIdByResponseEntity(Mockito.any(ResponseEntity.class));

      boolean triggerPDUAggregation = powerIQService.savePduAssetsToFlowgate(existedPduAssets, assetSource, powerIQAPIClient, location);
      TestCase.assertTrue(triggerPDUAggregation);
   }

   Pdu createPdu() {
      Pdu pdu = new Pdu();
      pdu.setName("pek-wor-pdu-02");
      pdu.setPhase("THREE_PHASE");
      InletReading inletReading = new InletReading();
      inletReading.setCurrent(1.2);
      inletReading.setApparentPower(20.0);
      inletReading.setVoltage(200.0);
      inletReading.setReadingTime("2018/10/18 05:57:26 +0300");
      inletReading.setActivePower(26.6);
      inletReading.setUnutilizedCapacity(15.2);
      inletReading.setInletId(127);
      inletReading.setInletOrdinal(1);
      List<InletReading> inletReadings = new ArrayList<InletReading>();
      inletReadings.add(inletReading);
      Reading pduReading = new Reading();
      pduReading.setInletReadings(inletReadings);

      List<InletPoleReading> inletPoleReadings = new ArrayList<InletPoleReading>();
      InletPoleReading poleReadingL1 = new InletPoleReading();
      poleReadingL1.setCurrent(1.62);
      poleReadingL1.setInletPoleOrdinal(1);
      poleReadingL1.setInletOrdinal(1);
      poleReadingL1.setReadingTime("2018/10/18 05:57:26 +0300");
      poleReadingL1.setUnutilizedCapacity(7.38);
      poleReadingL1.setVoltage(122.1);
      InletPoleReading poleReadingL2 = new InletPoleReading();
      poleReadingL2.setCurrent(3.62);
      poleReadingL2.setInletPoleOrdinal(2);
      poleReadingL2.setInletOrdinal(1);
      poleReadingL2.setReadingTime("2018/10/18 05:57:26 +0300");
      poleReadingL2.setUnutilizedCapacity(5.38);
      poleReadingL2.setVoltage(112.1);
      InletPoleReading poleReadingL3 = new InletPoleReading();
      poleReadingL3.setCurrent(0.62);
      poleReadingL3.setInletPoleOrdinal(3);
      poleReadingL3.setInletOrdinal(1);
      poleReadingL3.setReadingTime("2018/10/18 05:57:26 +0300");
      poleReadingL3.setUnutilizedCapacity(8.38);
      poleReadingL3.setVoltage(102.1);
      inletPoleReadings.add(poleReadingL1);
      inletPoleReadings.add(poleReadingL2);
      inletPoleReadings.add(poleReadingL3);
      pduReading.setInletPoleReadings(inletPoleReadings);
      pdu.setReading(pduReading);
      return pdu;
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
