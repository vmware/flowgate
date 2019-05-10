/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.FlowgateConstant;
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

   @Test
   public void testSummaryRepo() {
      deletedata();

      List<FacilitySoftwareConfig> facilities = new ArrayList<FacilitySoftwareConfig>();
      List<SDDCSoftwareConfig> sddcs = new ArrayList<SDDCSoftwareConfig>();
      List<ServerMapping> mappings = new ArrayList<ServerMapping>();
      List<Asset> assets = new ArrayList<Asset>();

      FacilitySoftwareConfig nlyte1 = createFacility();
      nlyte1.setType(com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType.Nlyte);
      FacilitySoftwareConfig powerIQ1 = createFacility();
      nlyte1.setType(com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType.PowerIQ);
      facilities.add(nlyte1);
      facilities.add(powerIQ1);
      facilityrepo.save(facilities);

      Asset nlyteServer1 = createAsset();
      nlyteServer1.setCategory(AssetCategory.Server);
      nlyteServer1.setAssetSource(nlyte1.getId());
      Asset nlyteServer2 = createAsset();
      nlyteServer2.setCategory(AssetCategory.Server);
      nlyteServer2.setAssetSource(nlyte1.getId());
      assets.add(nlyteServer1);
      assets.add(nlyteServer2);
      Asset nlytePDU1 = createAsset();
      nlytePDU1.setCategory(AssetCategory.PDU);
      nlytePDU1.setAssetSource(nlyte1.getId());
      Asset nlytePDU2 = createAsset();
      nlytePDU2.setCategory(AssetCategory.PDU);
      nlytePDU2.setAssetSource(nlyte1.getId());
      assets.add(nlytePDU1);
      assets.add(nlytePDU2);
      Asset powerIQSensor1 = createAsset();
      powerIQSensor1.setCategory(AssetCategory.Sensors);
      powerIQSensor1.setSubCategory(AssetSubCategory.Humidity);
      powerIQSensor1.setAssetSource(powerIQ1.getId());
      Asset powerIQSensor2 = createAsset();
      powerIQSensor2.setCategory(AssetCategory.Sensors);
      powerIQSensor2.setSubCategory(AssetSubCategory.Humidity);
      powerIQSensor2.setAssetSource(powerIQ1.getId());
      Asset powerIQSensor3 = createAsset();
      powerIQSensor3.setCategory(AssetCategory.Sensors);
      powerIQSensor3.setSubCategory(AssetSubCategory.Temperature);
      powerIQSensor3.setAssetSource(powerIQ1.getId());
      Asset powerIQSensor4 = createAsset();
      powerIQSensor4.setCategory(AssetCategory.Sensors);
      powerIQSensor4.setSubCategory(AssetSubCategory.Temperature);
      powerIQSensor4.setAssetSource(powerIQ1.getId());
      assets.add(powerIQSensor1);
      assets.add(powerIQSensor2);
      assets.add(powerIQSensor3);
      assets.add(powerIQSensor4);
      assetrepo.save(assets);


      SDDCSoftwareConfig vro1 =  createSDDC();
      vro1.setType(SoftwareType.VRO);
      SDDCSoftwareConfig vro2 =  createSDDC();
      vro2.setType(SoftwareType.VRO);
      SDDCSoftwareConfig vc1 =  createSDDC();
      vc1.setType(SoftwareType.VCENTER);
      SDDCSoftwareConfig vc2 =  createSDDC();
      vc2.setType(SoftwareType.VCENTER);
      sddcs.add(vro1);
      sddcs.add(vro2);
      sddcs.add(vc1);
      sddcs.add(vc2);
      sddcrepo.save(sddcs);

      ServerMapping server1 = createServerMapping();
      server1.setVroID(vro1.getId());
      ServerMapping server2 = createServerMapping();
      server2.setVroID(vro1.getId());
      ServerMapping server3 = createServerMapping();
      server3.setVcID(vc1.getId());
      ServerMapping server4 = createServerMapping();
      server4.setVcID(vc2.getId());
      ServerMapping server5 = createServerMapping();
      server5.setVroID(vro2.getId());

      mappings.add(server1);
      mappings.add(server2);
      mappings.add(server3);
      mappings.add(server4);
      mappings.add(server5);
      mappingrepo.save(mappings);

      try {
         Thread.sleep(3000);
      } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      SystemSummary data = summaryService.getSystemResult();
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
      deletedata();
   }

   public void deletedata() {
      PageRequest pageRequest = new PageRequest(FlowgateConstant.defaultPageNumber-1, FlowgateConstant.maxPageSize);
      Page<SDDCSoftwareConfig> sddcResult = sddcrepo.findAll(pageRequest);
      sddcrepo.delete(sddcResult.getContent());

      Page<FacilitySoftwareConfig> facilityResult = facilityrepo.findAll(pageRequest);
      facilityrepo.delete(facilityResult.getContent());

      Page<Asset> assetResult = assetrepo.findAll(pageRequest);
      assetrepo.delete(assetResult.getContent());

      Page<ServerMapping> serverMappingResult = mappingrepo.findAll(pageRequest);
      mappingrepo.delete(serverMappingResult.getContent());

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
