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
   public void queryHostNamesByIPTestZoneIsNull() {
      String ip = "10.161.71.154";
      List<InfoBloxIPInfoResult> expectedResult = this.getExpectedResult();
      InfobloxClient infobloxClient = new InfobloxClient(getInfobloxFacilitySoftware()[0]);
      infobloxClient = Mockito.spy(infobloxClient);
      Mockito.doReturn(this.getJsonResultForQueryHostNamesZoneIsNull()).when(infobloxClient).getHostNameList(ip);
      List<InfoBloxIPInfoResult> actualResult = infobloxClient.queryHostNamesByIP(ip);
      TestCase.assertEquals(expectedResult.size(), actualResult.size());
      for (int i = 0; i < expectedResult.size(); i++) {
         TestCase.assertEquals(expectedResult.get(i).getIpAddress(), actualResult.get(i).getIpAddress());
         TestCase.assertEquals(expectedResult.get(i).getHostName(), actualResult.get(i).getHostName());
         TestCase.assertEquals(expectedResult.get(i).getMacAddress(), actualResult.get(i).getMacAddress());
      }
   }

   @Test
   public void queryHostNamesByIPTestZoneNonNull() {
      String ip = "10.161.71.154";
      List<InfoBloxIPInfoResult> expectedResult = this.getExpectedResult();
      InfobloxClient infobloxClient = new InfobloxClient(getInfobloxFacilitySoftware()[0]);
      infobloxClient = Mockito.spy(infobloxClient);
      Mockito.doReturn(this.getJsonResultForQueryHostNamesZoneNonNull()).when(infobloxClient).getHostNameList(ip);
      List<InfoBloxIPInfoResult> actualResult = infobloxClient.queryHostNamesByIP(ip);
      TestCase.assertEquals(expectedResult.size(), actualResult.size());
      for (int i = 0; i < expectedResult.size(); i++) {
         TestCase.assertEquals(expectedResult.get(i).getIpAddress(), actualResult.get(i).getIpAddress());
         TestCase.assertEquals(expectedResult.get(i).getHostName(), actualResult.get(i).getHostName());
         TestCase.assertEquals(expectedResult.get(i).getMacAddress(), actualResult.get(i).getMacAddress());
      }
   }

   private JsonResultForQueryHostNames getJsonResultForQueryHostNamesZoneIsNull() {
      String resultJSON = "{\"result\":[{\"_ref\":\"ipv4address/Li5pcHY0X2FkZHJlc3MkMTAuMTYxLjcxLjE1NC8w:10.161.71.154\",\"discovered_data\":{\"first_discovered\":1603864985,\"last_discovered\":1603869896,\"mac_address\":\"00:50:56:be:60:62\",\"os\":\"Linux 3.10 - 4.1\"},\"ip_address\":\"10.161.71.154\",\"mac_address\":\"\",\"names\":[\"ubuntu01\"]}]}";
      JsonResultForQueryHostNames result = null;
      try {
         result = new ObjectMapper().readValue(resultJSON, JsonResultForQueryHostNames.class);
      } catch (JsonProcessingException e) {
         e.printStackTrace();
      }
      return result;
   }

   private JsonResultForQueryHostNames getJsonResultForQueryHostNamesZoneNonNull() {
      String resultJSON = "{\"result\":[{\"_ref\":\"ipv4address/Li5pcHY0X2FkZHJlc3MkMTAuMTYxLjcxLjE1NC8w:10.161.71.154\",\"discovered_data\":{\"first_discovered\":1603864985,\"last_discovered\":1603869896,\"mac_address\":\"00:50:56:be:60:62\",\"os\":\"Linux 3.10 - 4.1\"},\"ip_address\":\"10.161.71.154\",\"mac_address\":\"\",\"names\":[\"ubuntu01.info.com\"]}]}";
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
      infoBloxIPInfoResult1.setIpAddress("10.161.71.154");
      infoBloxIPInfoResult1.setHostName("ubuntu01");
      infoBloxIPInfoResult1.setMacAddress("00:50:56:be:60:62");
      expectedResult.add(infoBloxIPInfoResult1);
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
