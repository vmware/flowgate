/**
 * 
 */
package com.vmware.flowgate.vcworker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.vcworker.config.ServiceKeyConfig;
import com.vmware.flowgate.vcworker.scheduler.job.VCDataService;

import junit.framework.TestCase;

public class VCDataServiceTest {
   
   @Spy
   private VCDataService service = new VCDataService();
    
   @Spy
   @InjectMocks
   private WormholeAPIClient wormholeAPIClient;

   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
   }
   
   @Test
   public void testGetPDUSwitchIDNamePortMapping() {
      Asset asset = new Asset();
      HashMap<String, String> justifications = new HashMap<String, String>();
      justifications.put("NETWORK_PORT_FOR_SERVER",
            "pci-2:hba:1_FIELDSPLIT_cloud-fc02-sha1_FIELDSPLIT_05_FIELDSPLIT_4b029b8337c64630b68d0f6c20a18e40,onboard:1gb-nic:4_FIELDSPLIT_cloud-sw02-sha1_FIELDSPLIT_08_FIELDSPLIT_3fc319e50d21476684d841aa0842bd52,pci-1:hba:1_FIELDSPLIT_cloud-fc01-sha1_FIELDSPLIT_05_FIELDSPLIT_5008de702d7f4a96af939609c5453ec5,ilo_FIELDSPLIT_cloud-sw03-sha1_FIELDSPLIT_02_FIELDSPLIT_e53c01312682455ab8c039780c88db6f,onboard:1gb-nic:1_FIELDSPLIT_cloud-sw01-sha1_FIELDSPLIT_07_FIELDSPLIT_3590c57182fe481d98d9ff647abaebc6,onboard:1gb-nic:3_FIELDSPLIT_cloud-sw01-sha1_FIELDSPLIT_08_FIELDSPLIT_3590c57182fe481d98d9ff647abaebc6,onboard:1gb-nic:2_FIELDSPLIT_cloud-sw02-sha1_FIELDSPLIT_07_FIELDSPLIT_3fc319e50d21476684d841aa0842bd52");
      asset.setJustificationfields(justifications);
      VCDataService service = new VCDataService();
      Map<String, String> nameMap = service.getPDUSwitchIDNamePortMapping(asset);
      Assert.assertTrue(nameMap.containsKey("e53c01312682455ab8c039780c88db6f"));
      Assert.assertTrue(
            nameMap.get("4b029b8337c64630b68d0f6c20a18e40").equals("cloud-fc02-sha1:05"));
      Assert.assertEquals(5, nameMap.size());
   }
   
   @Test
   public void testCheckAndUpdateIntegrationStatus() {

      SDDCSoftwareConfig vc = Mockito.spy(new SDDCSoftwareConfig());
      IntegrationStatus integrationStatus = Mockito.spy(new IntegrationStatus());
      String message = "message";
      
      vc.setIntegrationStatus(null);
      Mockito.doNothing().when(service).updateIntegrationStatus(any(SDDCSoftwareConfig.class));
      service.checkAndUpdateIntegrationStatus(vc, message);
      TestCase.assertEquals(1, vc.getIntegrationStatus().getRetryCounter());
      
      Mockito.when(vc.getIntegrationStatus()).thenReturn(integrationStatus);
      Mockito.when(integrationStatus.getRetryCounter()).thenReturn(FlowgateConstant.MAXNUMBEROFRETRIES);
      
      service.checkAndUpdateIntegrationStatus(vc, message);
      TestCase.assertEquals(IntegrationStatus.Status.ERROR, integrationStatus.getStatus());
      TestCase.assertEquals(message, integrationStatus.getDetail());

   }

}
