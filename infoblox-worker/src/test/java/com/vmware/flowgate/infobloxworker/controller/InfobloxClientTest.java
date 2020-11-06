/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.flowgate.infobloxworker.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.infobloxworker.model.InfoBloxIPInfoResult;
import com.vmware.flowgate.infobloxworker.model.JsonResultForQueryHostNames;
import com.vmware.flowgate.infobloxworker.service.InfobloxClient;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class InfobloxClientTest {

   @Test
   public void queryHostNamesByIPTest() {
      String ip = "10.161.71.154";
      List<InfoBloxIPInfoResult> expectedResult = this.getExpectedResult();
      InfobloxClient infobloxClient = new InfobloxClient(getInfobloxFacilitySoftware()[0]);
      infobloxClient = Mockito.spy(infobloxClient);
      Mockito.doReturn(this.getJsonResultForQueryHostNames()).when(infobloxClient).getHostNameList(ip);
      List<InfoBloxIPInfoResult> actualResult = infobloxClient.queryHostNamesByIP(ip);
      TestCase.assertEquals(expectedResult.size(), actualResult.size());
      for (int i = 0; i < expectedResult.size(); i++) {
         TestCase.assertEquals(expectedResult.get(i).getIpAddress(), actualResult.get(i).getIpAddress());
         TestCase.assertEquals(expectedResult.get(i).getHostName(), actualResult.get(i).getHostName());
         TestCase.assertEquals(expectedResult.get(i).getMacAddress(), actualResult.get(i).getMacAddress());
      }
   }

   private JsonResultForQueryHostNames getJsonResultForQueryHostNames() {
      String resultJSON = "{\"result\":[{\"_ref\":\"record:host/ZG5zLmhvc3QkLl9kZWZhdWx0LmNvbS5pbmZvLjEudGVzdC50ZXN0:test.test.1.info.com/default\",\"ipv4addrs\":[{\"_ref\":\"record:host_ipv4addr/ZG5zLmhvc3RfYWRkcmVzcyQuX2RlZmF1bHQuY29tLmluZm8uMS50ZXN0LnRlc3QuMTkyLjE2OC4yLjEu:192.168.2.1/test.test.1.info.com/default\",\"host\":\"test.test.1.info.com\",\"ipv4addr\":\"192.168.2.1\",\"mac\":\"22:53:25:ae:60:61\"}],\"name\":\"test.test.1.info.com\",\"zone\":\"info.com\"},{\"_ref\":\"record:host/ZG5zLmhvc3QkLl9kZWZhdWx0LmNvbS52bXcudGVzdDE2OA:test168.vmw.com/default\",\"ipv4addrs\":[{\"_ref\":\"record:host_ipv4addr/ZG5zLmhvc3RfYWRkcmVzcyQuX2RlZmF1bHQuY29tLnZtdy50ZXN0MTY4LjE5Mi4xNjguMi4xLg:192.168.2.1/test168.vmw.com/default\",\"host\":\"test168.vmw.com\",\"ipv4addr\":\"192.168.2.1\",\"mac\":\"89:53:54:ae:61:60\"}],\"name\":\"test168.vmw.com\",\"zone\":\"vmw.com\"}]}";
      JsonResultForQueryHostNames result = null;
      try {
         result = new ObjectMapper().readValue(resultJSON, JsonResultForQueryHostNames.class);
      } catch (JsonProcessingException e) {
         e.printStackTrace();
      }
      return result;
   }

   private List<InfoBloxIPInfoResult> getExpectedResult() {
      List<InfoBloxIPInfoResult> expectedResult = new ArrayList<>();
      InfoBloxIPInfoResult infoBloxIPInfoResult1 = new InfoBloxIPInfoResult();
      infoBloxIPInfoResult1.setIpAddress("192.168.2.1");
      infoBloxIPInfoResult1.setHostName("test.test.1");
      infoBloxIPInfoResult1.setMacAddress("22:53:25:ae:60:61");
      InfoBloxIPInfoResult infoBloxIPInfoResult2 = new InfoBloxIPInfoResult();
      infoBloxIPInfoResult2.setIpAddress("192.168.2.1");
      infoBloxIPInfoResult2.setHostName("test168");
      infoBloxIPInfoResult2.setMacAddress("89:53:54:ae:61:60");
      expectedResult.add(infoBloxIPInfoResult1);
      expectedResult.add(infoBloxIPInfoResult2);
      return expectedResult;
   }

   private FacilitySoftwareConfig[] getInfobloxFacilitySoftware() {
      FacilitySoftwareConfig[] facilitySoftwareConfigs = new FacilitySoftwareConfig[1];
      FacilitySoftwareConfig facilitySoftwareConfig = new FacilitySoftwareConfig();
      facilitySoftwareConfig.setPassword("O75xginpkAD748w=Lc20CrTzd1lEpvDTdJqH5IXBZTb5gYp7P8awDAs19F0=");
      facilitySoftwareConfig.setServerURL("https://10.161.71.133");
      facilitySoftwareConfig.setName("infoblox-1");
      facilitySoftwareConfig.setVerifyCert(false);
      IntegrationStatus integrationStatus = new IntegrationStatus();
      integrationStatus.setRetryCounter(0);
      integrationStatus.setDetail("");
      integrationStatus.setStatus(IntegrationStatus.Status.ACTIVE);
      facilitySoftwareConfig.setIntegrationStatus(integrationStatus);
      facilitySoftwareConfig.setUserName("admin");
      facilitySoftwareConfig.setType(FacilitySoftwareConfig.SoftwareType.InfoBlox);
      facilitySoftwareConfig.setUserId("e1edfv8953002379827896a1aaiqoose");
      facilitySoftwareConfigs[0] = facilitySoftwareConfig;
      return facilitySoftwareConfigs;
   }

}
