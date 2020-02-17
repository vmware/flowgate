package com.vmware.flowgate.poweriqworker.jobtest;

import static org.mockito.Matchers.any;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.poweriqworker.client.PowerIQAPIClient;
import com.vmware.flowgate.poweriqworker.jobs.PowerIQService;
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

}
