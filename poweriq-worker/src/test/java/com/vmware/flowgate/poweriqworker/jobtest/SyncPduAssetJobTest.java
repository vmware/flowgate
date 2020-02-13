package com.vmware.flowgate.poweriqworker.jobtest;

import static org.mockito.Matchers.any;

import java.io.IOException;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.poweriqworker.client.PowerIQAPIClient;
import com.vmware.flowgate.poweriqworker.jobs.PowerIQService;
import com.vmware.flowgate.poweriqworker.model.Inlet;
import com.vmware.flowgate.poweriqworker.model.Outlet;
import com.vmware.flowgate.poweriqworker.model.Pdu;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SyncPduAssetJobTest {

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
   public void testGetAllPduAssetsFromPowerIQ() {
      Mockito.when(this.powerIQAPIClient.getPdus(100,0)).thenReturn(getPdus());
      Mockito.when(this.powerIQAPIClient.getPdus(100,100)).thenReturn(null);
      Mockito.when(this.powerIQAPIClient.getInlets(128)).thenReturn(getInlets());
      Mockito.when(this.powerIQAPIClient.getOutlets(128)).thenReturn(getOutlets());
      powerIQService.getLocationInfo(this.powerIQAPIClient);
      List<Asset> assets = powerIQService.getAllPduAssetsFromPowerIQ("po09imkhdplbvf540fwusy67n", this.powerIQAPIClient);
      for(Asset asset : assets) {
         if(asset.getAssetName().equals("Pdu1")) {
            TestCase.assertEquals("plbvf540", asset.getSerialnumber());
            ObjectMapper mapper = new ObjectMapper();
            String pduInfo = asset.getJustificationfields().get(FlowgateConstant.PDU);
            Map<String, String> map = null;
            try {
               map = mapper.readValue(pduInfo, new TypeReference<Map<String,String>>() {});
            } catch (IOException e) {
               e.printStackTrace();
            }
            TestCase.assertEquals("34", map.get(FlowgateConstant.PDU_RATE_AMPS));
            TestCase.assertEquals("6.4", map.get(FlowgateConstant.PDU_MIN_RATE_POWER));
            TestCase.assertEquals("7.7", map.get(FlowgateConstant.PDU_MAX_RATE_POWER));
            TestCase.assertEquals("220", map.get(FlowgateConstant.PDU_MIN_RATE_VOLTS));
            TestCase.assertEquals("240", map.get(FlowgateConstant.PDU_MAX_RATE_VOLTS));
         }
      }
   }

   @Test
   public void testGetPduAssetsNeedtoSave() {
      Asset asset = createAsset();
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Asset> existedMap = new HashMap<String, Asset>();
      existedMap.put("128", asset);

      Asset newAsset = createAsset();
      newAsset.setAssetName("pek-pdu-02");
      newAsset.setSerialnumber("l9i8728d5");
      List<Asset> assetsFromPowerIQ = new ArrayList<Asset>();
      Map<String,String> newAssetpduInfo = new HashMap<String,String>();
      newAssetpduInfo.put(FlowgateConstant.PDU_ID_FROM_POWERIQ, "128");
      newAssetpduInfo.put(FlowgateConstant.PDU_RATE_AMPS, "34");
      newAssetpduInfo.put(FlowgateConstant.PDU_MAX_RATE_POWER, "7.7");
      String pduInfoString = null;
      try {
         pduInfoString = mapper.writeValueAsString(newAssetpduInfo);
      } catch (JsonProcessingException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      HashMap<String,String> justficationfields = new HashMap<String,String>();
      justficationfields.put(FlowgateConstant.PDU, pduInfoString);
      newAsset.setJustificationfields(justficationfields);
      assetsFromPowerIQ.add(newAsset);
      List<Asset> assets = powerIQService.getPduAssetsNeedtoSave(existedMap,assetsFromPowerIQ);

      for(Asset asset1 : assets) {
         if(asset1.getAssetName().equals(newAsset.getAssetName())) {
            TestCase.assertEquals(newAsset.getSerialnumber(), asset1.getSerialnumber());
            String pduInfoStrings = asset1.getJustificationfields().get(FlowgateConstant.PDU);
            Map<String, String> map = null;
            try {
               map = mapper.readValue(pduInfoStrings, new TypeReference<Map<String,String>>() {});
            } catch (IOException e) {
               e.printStackTrace();
            }
            TestCase.assertEquals(newAssetpduInfo.get(FlowgateConstant.PDU_RATE_AMPS), map.get(FlowgateConstant.PDU_RATE_AMPS));
            TestCase.assertEquals(newAssetpduInfo.get(FlowgateConstant.PDU_MAX_RATE_POWER), map.get(FlowgateConstant.PDU_MAX_RATE_POWER));
         }
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

   List<Pdu> getPdus() {
      List<Pdu> pdus = new ArrayList<Pdu>();
      Pdu pdu = new Pdu();
      pdu.setName("Pdu1");
      pdu.setRatedAmps("34A");
      pdu.setRatedVa("6.4-7.7kVA");
      pdu.setRatedVolts("220-240V");
      pdu.setId(128);
      pdu.setSerialNumber("plbvf540");
      pdus.add(pdu);
      return pdus;
   }

   List<Outlet> getOutlets() {
      Outlet outlet = new Outlet();
      List<Outlet> outlets = new ArrayList<Outlet>();
      outlet.setId(12L);
      outlet.setName("Outlet1");
      outlet.setOrdinal(1L);
      outlet.setPduId(128L);
      outlet.setRatedAmps(10.2);
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
      inlets.add(inlet);
      return inlets;
   }
}
