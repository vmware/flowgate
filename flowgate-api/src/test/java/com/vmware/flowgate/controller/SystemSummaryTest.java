/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;


import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.SystemSummary;
import com.vmware.flowgate.repository.AssetRepository;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.repository.SDDCSoftwareRepository;
import com.vmware.flowgate.repository.ServerMappingRepository;
import com.vmware.flowgate.repository.SystemSummaryRepository;
import com.vmware.flowgate.service.SummaryService;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class SystemSummaryTest {

   @Autowired
   CouchbaseTemplate couchbaseTemplate;

   @Autowired
   SystemSummaryRepository systemSummaryrepo;

   @Autowired
   SDDCSoftwareRepository sddcrepo;

   @Autowired
   FacilitySoftwareConfigRepository facilityrepo;

   @Autowired
   ServerMappingRepository mappingrepo;

   @Autowired
   AssetRepository assetrepo;

   @Autowired
   SummaryService summaryService;

   @MockBean
   private StringRedisTemplate template;

   @Test
   public void testSummaryRepo() throws IOException {
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      FacilitySoftwareConfig nlyte1 = createFacility();
      nlyte1.setType(com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType.Nlyte);
      nlyte1 = facilityrepo.save(nlyte1);

      FacilitySoftwareConfig powerIQ1 = createFacility();
      powerIQ1.setType(com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType.PowerIQ);
      powerIQ1 = facilityrepo.save(powerIQ1);

      Asset nlyteServer1 = createAsset();
      nlyteServer1.setCategory(AssetCategory.Server);
      nlyteServer1.setAssetSource(nlyte1.getId());
      nlyteServer1 = assetrepo.save(nlyteServer1);
      Asset nlyteServer2 = createAsset();
      nlyteServer2.setCategory(AssetCategory.Server);
      nlyteServer2.setAssetSource(nlyte1.getId());
      nlyteServer2 = assetrepo.save(nlyteServer2);

      Asset nlytePDU1 = createAsset();
      nlytePDU1.setCategory(AssetCategory.PDU);
      nlytePDU1.setAssetSource(nlyte1.getId());
      nlytePDU1 = assetrepo.save(nlytePDU1);
      Asset nlytePDU2 = createAsset();
      nlytePDU2.setCategory(AssetCategory.PDU);
      nlytePDU2.setAssetSource(nlyte1.getId());
      nlytePDU2 = assetrepo.save(nlytePDU2);
      Asset powerIQSensor1 = createAsset();
      powerIQSensor1.setCategory(AssetCategory.Sensors);
      powerIQSensor1.setSubCategory(AssetSubCategory.Humidity);
      powerIQSensor1.setAssetSource(powerIQ1.getId());
      powerIQSensor1 = assetrepo.save(powerIQSensor1);
      Asset powerIQSensor2 = createAsset();
      powerIQSensor2.setCategory(AssetCategory.Sensors);
      powerIQSensor2.setSubCategory(AssetSubCategory.Humidity);
      powerIQSensor2.setAssetSource(powerIQ1.getId());
      powerIQSensor2 = assetrepo.save(powerIQSensor2);
      Asset powerIQSensor3 = createAsset();
      powerIQSensor3.setCategory(AssetCategory.Sensors);
      powerIQSensor3.setSubCategory(AssetSubCategory.Temperature);
      powerIQSensor3.setAssetSource(powerIQ1.getId());
      powerIQSensor3 = assetrepo.save(powerIQSensor3);
      Asset powerIQSensor4 = createAsset();
      powerIQSensor4.setCategory(AssetCategory.Sensors);
      powerIQSensor4.setSubCategory(AssetSubCategory.Temperature);
      powerIQSensor4.setAssetSource(powerIQ1.getId());
      powerIQSensor4 = assetrepo.save(powerIQSensor4);

      SDDCSoftwareConfig vro1 =  createSDDC();
      vro1.setType(SoftwareType.VRO);
      vro1 = sddcrepo.save(vro1);
      SDDCSoftwareConfig vro2 =  createSDDC();
      vro2.setType(SoftwareType.VRO);
      vro2 = sddcrepo.save(vro2);
      SDDCSoftwareConfig vc1 =  createSDDC();
      vc1.setType(SoftwareType.VCENTER);
      vc1 = sddcrepo.save(vc1);
      SDDCSoftwareConfig vc2 =  createSDDC();
      vc2.setType(SoftwareType.VCENTER);
      vc2 = sddcrepo.save(vc2);

      ServerMapping server1 = createServerMapping();
      server1.setVroID(vro1.getId());
      server1 = mappingrepo.save(server1);
      ServerMapping server2 = createServerMapping();
      server2.setVroID(vro1.getId());
      server2 = mappingrepo.save(server2);
      ServerMapping server3 = createServerMapping();
      server3.setVcID(vc1.getId());
      server3 = mappingrepo.save(server3);
      ServerMapping server4 = createServerMapping();
      server4.setVcID(vc2.getId());
      server4 = mappingrepo.save(server4);
      ServerMapping server5 = createServerMapping();
      server5.setVroID(vro2.getId());
      server5 = mappingrepo.save(server5);

      try {
         Thread.sleep(3000);
      } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      try {
         SystemSummary data = summaryService.getSystemResult(false);
         TestCase.assertEquals(8, data.getAssetsNum());
         TestCase.assertEquals(2, data.getFacilitySystemNum());
         TestCase.assertEquals(4, data.getSddcIntegrationNum());
         TestCase.assertEquals(2, data.getHumiditySensorNum());
         TestCase.assertEquals(2, data.getTemperatureSensorNum());
         TestCase.assertEquals(4, data.getSensorNum());
         TestCase.assertEquals(2, data.getPduNum());
         TestCase.assertEquals(2, data.getServerNum());
         TestCase.assertEquals(5, data.getSddcServerNum());
         TestCase.assertEquals(2, data.getVroNum());
         TestCase.assertEquals(2, data.getVcNum());
      }finally {
         facilityrepo.deleteById(nlyte1.getId());
         facilityrepo.deleteById(powerIQ1.getId());
         assetrepo.deleteById(nlyteServer1.getId());
         assetrepo.deleteById(nlyteServer2.getId());
         assetrepo.deleteById(nlytePDU1.getId());
         assetrepo.deleteById(nlytePDU2.getId());
         assetrepo.deleteById(powerIQSensor1.getId());
         assetrepo.deleteById(powerIQSensor2.getId());
         assetrepo.deleteById(powerIQSensor3.getId());
         assetrepo.deleteById(powerIQSensor4.getId());
         sddcrepo.deleteById(vc1.getId());
         sddcrepo.deleteById(vc2.getId());
         sddcrepo.deleteById(vro1.getId());
         sddcrepo.deleteById(vro2.getId());
         mappingrepo.deleteById(server1.getId());
         mappingrepo.deleteById(server2.getId());
         mappingrepo.deleteById(server3.getId());
         mappingrepo.deleteById(server4.getId());
         mappingrepo.deleteById(server5.getId());
      }
   }

   Asset createAsset() {
      Asset asset = new Asset();
      asset.setId(UUID.randomUUID().toString());
      return asset;
   }

   SDDCSoftwareConfig createSDDC() {
      SDDCSoftwareConfig sddc = new SDDCSoftwareConfig();
      sddc.setId(UUID.randomUUID().toString());
      return sddc;
   }

   FacilitySoftwareConfig createFacility() {
      FacilitySoftwareConfig facility = new FacilitySoftwareConfig();
      facility.setId(UUID.randomUUID().toString());
      return facility;
   }

   ServerMapping createServerMapping() {
      ServerMapping serverMapping = new ServerMapping();
      serverMapping.setId(UUID.randomUUID().toString());
      return serverMapping;
   }
}
