/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.labsdb.jobtest;

import static org.mockito.Matchers.anyString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import com.vmware.wormhole.client.WormholeAPIClient;
import com.vmware.wormhole.common.AssetCategory;
import com.vmware.wormhole.common.AssetStatus;
import com.vmware.wormhole.common.NetworkMapping;
import com.vmware.wormhole.common.PduMapping;
import com.vmware.wormhole.common.WormholeConstant;
import com.vmware.wormhole.common.model.Asset;
import com.vmware.wormhole.labsdb.client.LabsdbClient;
import com.vmware.wormhole.labsdb.common.EndDevice;
import com.vmware.wormhole.labsdb.config.ServiceKeyConfig;
import com.vmware.wormhole.labsdb.job.LabsdbService;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SyncWiremapDataJobTest {

   @Mock
   private WormholeAPIClient wormholeAPIClient;
   @Mock
   private ServiceKeyConfig config;
   @Mock
   private LabsdbClient labsdbClient;
   @Spy
   @InjectMocks
   private LabsdbService labsdbService;
   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
      
   }
   
   @Test
   public void getAssetNameIDMapTest() {
      Map<String,String> serverMap = labsdbService.getAssetNameIDMap(null);
      TestCase.assertEquals(true, serverMap.isEmpty());
   }
   
   @Test
   public void getAssetNameIDMapTest1() {
      Mockito.when(this.wormholeAPIClient.getAssetsByType(AssetCategory.Server)).thenReturn(getAssets(AssetCategory.Server));
      Map<String,String> serverMap = labsdbService.getAssetNameIDMap(AssetCategory.Server);
      for(Map.Entry<String, String> map:serverMap.entrySet()) {
         String value = map.getKey();
         if(value.equals("w1-eeqa-fas3250-01")) {
            TestCase.assertEquals("5c778c598ecf859960e2d395", map.getValue());
         }
      }
     
   }
   
   @Test
   public void filterServersTest1() {
      List<Asset> servers = Arrays.asList(getAssets(AssetCategory.Server).getBody());
      List<Asset> assets = labsdbService.filterServers(servers);
      TestCase.assertEquals(1, assets.size());
      TestCase.assertEquals("w1-eeqa-fas3250-01", assets.get(0).getAssetName());
   }
   
   //PduMappingStatus and NetworkMappingStatus is null
   @Test
   public void filterServersTest2() {
      List<Asset> servers = new ArrayList<Asset>();
      Asset asset1 = new Asset();
      asset1.setId("5c778c598ecf859960e2c2a0");
      asset1.setAssetName("w1-lil-c-016");
      asset1.setAssetNumber(33503);
      asset1.setAssetSource("5c74da749662e398e01f665f");
      asset1.setCategory(AssetCategory.Server);
      asset1.setModel("Dell");
      asset1.setManufacturer("Dell");
      AssetStatus assetstatus = new AssetStatus();
      assetstatus.setStatus(AssetStatus.Status.Active);
      asset1.setStatus(assetstatus);
      
      Asset asset = new Asset();
      asset.setId("5c778c598ecf859960e2d395");
      asset.setAssetName("w1-eeqa-fas3250-01");
      asset.setAssetNumber(53135);
      asset.setAssetSource("5c74da749662e398e01f665f");
      asset.setCategory(AssetCategory.Server);
      asset.setModel("NetApp");
      asset.setManufacturer("NetApp");
      servers.add(asset);
      servers.add(asset1);
      List<Asset> assetResult = labsdbService.filterServers(servers);
      TestCase.assertEquals(2, assetResult.size());
      for(Asset asset2:assetResult) {
         if(asset2.getAssetName().equals("w1-lil-c-016")) {
            TestCase.assertEquals("5c778c598ecf859960e2c2a0", asset2.getId());
         }
      }
   }
   
   //PduMappingStatus or NetworkMappingStatus is null
   @Test
   public void filterServersTest3() {
      List<Asset> servers = new ArrayList<Asset>();
      Asset asset1 = new Asset();
      asset1.setId("5c778c598ecf859960e2c2a0");
      asset1.setAssetName("w1-lil-c-016");
      asset1.setAssetNumber(33503);
      asset1.setAssetSource("5c74da749662e398e01f665f");
      asset1.setCategory(AssetCategory.Server);
      asset1.setModel("Dell");
      asset1.setManufacturer("Dell");
      AssetStatus assetstatus = new AssetStatus();
      assetstatus.setStatus(AssetStatus.Status.Active);
      assetstatus.setPduMapping(PduMapping.MAPPEDBYLABSDB);
      asset1.setStatus(assetstatus);
      
      Asset asset = new Asset();
      asset.setId("5c778c598ecf859960e2d395");
      asset.setAssetName("w1-eeqa-fas3250-01");
      asset.setAssetNumber(53135);
      asset.setAssetSource("5c74da749662e398e01f665f");
      asset.setCategory(AssetCategory.Server);
      asset.setModel("NetApp");
      asset.setManufacturer("NetApp");
      AssetStatus status = new AssetStatus();
      status.setStatus(AssetStatus.Status.Active);
      status.setNetworkMapping(NetworkMapping.MAPPEDBYLABSDB);
      asset.setStatus(status);
      servers.add(asset);
      servers.add(asset1);
      
      List<Asset> assetResult = labsdbService.filterServers(servers);
      TestCase.assertEquals(2, assetResult.size());
      for(Asset asset2:assetResult) {
         if(asset2.getAssetName().equals("w1-eeqa-fas3250-01")) {
            TestCase.assertEquals("5c778c598ecf859960e2d395", asset2.getId());
         }
      }
   }
   
   //PduMappingStatus and NetworkMappingStatus is not null
   @Test
   public void filterServersTest4() {
      List<Asset> servers = new ArrayList<Asset>();
      Asset asset1 = new Asset();
      asset1.setId("5c778c598ecf859960e2c2a0");
      asset1.setAssetName("w1-lil-c-016");
      asset1.setAssetNumber(33503);
      asset1.setAssetSource("5c74da749662e398e01f665f");
      asset1.setCategory(AssetCategory.Server);
      asset1.setModel("Dell");
      asset1.setManufacturer("Dell");
      AssetStatus assetstatus = new AssetStatus();
      assetstatus.setStatus(AssetStatus.Status.Active);
      assetstatus.setPduMapping(PduMapping.MAPPEDBYLABSDB);
      assetstatus.setNetworkMapping(NetworkMapping.MAPPEDBYLABSDB);
      asset1.setStatus(assetstatus);
      
      Asset asset = new Asset();
      asset.setId("5c778c598ecf859960e2d395");
      asset.setAssetName("w1-eeqa-fas3250-01");
      asset.setAssetNumber(53135);
      asset.setAssetSource("5c74da749662e398e01f665f");
      asset.setCategory(AssetCategory.Server);
      asset.setModel("NetApp");
      asset.setManufacturer("NetApp");
      AssetStatus status = new AssetStatus();
      status.setStatus(AssetStatus.Status.Active);
      status.setNetworkMapping(NetworkMapping.MAPPEDBYLABSDB);
      asset.setStatus(status);
      servers.add(asset);
      servers.add(asset1);
      
      List<Asset> assetResult = labsdbService.filterServers(servers);
      TestCase.assertEquals(1, assetResult.size());
      for(Asset asset2:assetResult) {
         if(asset2.getAssetName().equals("w1-eeqa-fas3250-01")) {
            TestCase.assertEquals("5c778c598ecf859960e2d395", asset2.getId());
         }
      }
   }
   
   @Test
   public void generatorWiremapDataTest() {
      ResponseEntity<Asset[]> result = getAssets(AssetCategory.Server);
      Map<String,String> network = new HashMap<String,String>();
      network.put("sin2-build-rdev1", "5c778c598ecf859960e2be30");
      Map<String,String> pdu = new HashMap<String,String>();
      pdu.put("w3r17c05-pdu4", "wertbhukloiup5996q23df530");
      Mockito.when(this.labsdbClient.getWireMap(anyString())).thenReturn(getWiremap());
      List<Asset> assets = labsdbService.generatorWiremapData(Arrays.asList(result.getBody()),pdu, network, labsdbClient);
      for(Asset asset:assets) {
         if(asset.getAssetName().equals("w1-eeqa-fas3250-01")) {
            List<String> networks = asset.getSwitches();
            TestCase.assertEquals("5c778c598ecf859960e2be30", networks.get(0));
            String device = asset.getJustificationfields().get(WormholeConstant.NETWORK_PORT_FOR_SERVER);
            TestCase.assertEquals("01"+WormholeConstant.SEPARATOR+"sin2-build-rdev1"+WormholeConstant.SEPARATOR+"onboard-1"+
            WormholeConstant.SEPARATOR+""+"5c778c598ecf859960e2be30", device);
         }
      }
   }
   
   @Test
   public void generatorWiremapDataTest1() {
      ResponseEntity<Asset[]> result = getAssets(AssetCategory.Server);
      EndDevice net1 = new EndDevice();
      net1.setEndDeviceAssetId("5c8749469662e32e2470d654");
      net1.setEndDeviceName("w4-pek2");
      net1.setEndPort("onboard-1");
      net1.setStartPort("03");
      
      EndDevice net2 = new EndDevice();
      net2.setEndDeviceAssetId("5c778c598ecf859960e2be30");
      net2.setEndDeviceName("sin2-build-rdev1");
      net2.setEndPort("onboard-1");
      net2.setStartPort("01");
      
      HashSet<String> nets = new HashSet<String>();
      Asset assetNet = result.getBody()[0];
      HashMap<String,String> fileds = new HashMap<String,String>();
      nets.add(net1.toString());
      nets.add(net2.toString());
      fileds.put(WormholeConstant.NETWORK_PORT_FOR_SERVER, String.join(WormholeConstant.SPILIT_FLAG, nets));
      assetNet.setJustificationfields(fileds);
      
      Map<String,String> network = new HashMap<String,String>();
      network.put("sin2-build-rdev1", "5c778c598ecf859960e2be30");
      Map<String,String> pdu = new HashMap<String,String>();
      pdu.put("w3r17c05-pdu4", "wertbhukloiup5996q23df530");
      Mockito.when(this.labsdbClient.getWireMap(anyString())).thenReturn(getWiremap());
      List<Asset> assets = labsdbService.generatorWiremapData(Arrays.asList(result.getBody()),pdu, network, labsdbClient);
      
      for(Asset asset:assets) {
         if(asset.getAssetName().equals("w1-eeqa-fas3250-01")) {
            List<String> networks = asset.getSwitches();
            TestCase.assertEquals("5c778c598ecf859960e2be30", networks.get(0));
            String networkdevices = 
                  asset.getJustificationfields().get(WormholeConstant.NETWORK_PORT_FOR_SERVER);
            String expectValue = String.join(WormholeConstant.SPILIT_FLAG, nets);
            TestCase.assertEquals(expectValue, networkdevices);
         }
      }
   }
   
   public ResponseEntity<String> getWiremap(){
     String result = "<RESULTS>"
           + "<DEVICE name=\"s4048-sin2-cw16\">"
           + "<PORT name=\"01\"><DB_SOURCE>labsdb.eng.vmware.com</DB_SOURCE>"
           + "<WIRE_TYPE>net</WIRE_TYPE>"
           + "<CONNECTS>sin2-build-rdev1:onboard-1</CONNECTS>"
           + "</PORT>"
           + "<PORT name=\"02\">"
           + "<DB_SOURCE>labsdb.eng.vmware.com</DB_SOURCE>"
           + "<WIRE_TYPE>pdu</WIRE_TYPE>"
           + "<CONNECTS>w3r17c05-pdu4:onboard-5</CONNECTS>"
           + "</PORT></DEVICE></RESULTS>"; 
     return new ResponseEntity<String>(result, HttpStatus.OK);
   }
   
   public ResponseEntity<Asset[]> getAssets(AssetCategory category) {
      Asset[] assets = new Asset[1];
      switch (category) {
      case Networks:
         assets[0] = createNetWork();
         break;
      case Server:
         assets[0] = createServer();
         break;
      case PDU:
         assets[0] = createPdu();
         break;
      default:
         break;
      }
      return new ResponseEntity<Asset[]>(assets, HttpStatus.OK);
   }

   Asset createServer() {
      Asset asset = new Asset();
      asset.setId("5c778c598ecf859960e2d395");
      asset.setAssetName("w1-eeqa-fas3250-01");
      asset.setAssetNumber(53135);
      asset.setAssetSource("5c74da749662e398e01f665f");
      asset.setCategory(AssetCategory.Server);
      asset.setModel("NetApp");
      asset.setManufacturer("NetApp");
      return asset;
   }
   
   Asset createNetWork() {
      Asset asset = new Asset();
      asset.setAssetName("w1s04-edge-x450-1");
      asset.setAssetNumber(6443);
      asset.setAssetSource("5c74da749662e398e01f665f");
      asset.setCategory(AssetCategory.Networks);
      asset.setModel("Extreme Networks");
      asset.setManufacturer("Extreme Networks Summit X450a-48t");
      return asset;
   }
   
   Asset createPdu() {
      Asset asset = new Asset();
      asset.setAssetName("ams5-k03-b-pdu");
      asset.setAssetNumber(328076);
      asset.setAssetSource("5c74da749662e398e01f665f");
      asset.setCategory(AssetCategory.PDU);
      return asset;
   }

}
