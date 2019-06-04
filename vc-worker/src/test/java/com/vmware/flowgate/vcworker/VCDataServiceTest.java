/**
 * 
 */
package com.vmware.flowgate.vcworker;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.vcworker.scheduler.job.VCDataService;

public class VCDataServiceTest {

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
}
