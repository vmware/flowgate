package com.vmware.flowgate.infobloxworker.controller;

import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.infobloxworker.config.ServiceKeyConfig;
import com.vmware.flowgate.infobloxworker.jobs.InfoBloxService;
import com.vmware.flowgate.infobloxworker.model.DiscoveredData;
import com.vmware.flowgate.infobloxworker.model.InfoBloxIPInfoResult;
import com.vmware.flowgate.infobloxworker.model.InfobloxIpv4addressItem;
import com.vmware.flowgate.infobloxworker.redis.TestRedisConfiguration;
import com.vmware.flowgate.infobloxworker.service.InfobloxClient;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestRedisConfiguration.class)
@ActiveProfiles("test")
public class InfobloxServiceTest {

   @Spy
   @InjectMocks
   private InfoBloxService infoBloxService;
   @Mock
   private InfobloxClient infobloxClient;
   @Mock
   private WormholeAPIClient wormholeAPIClient;
   @Mock
   private ServiceKeyConfig serviceKeyConfig;

   @Before
   public void before() {
      Mockito.doReturn("FLOWGATETEST").when(serviceKeyConfig).getServiceKey();
      Mockito.doNothing().when(wormholeAPIClient).setServiceKey(Mockito.anyString());
      Mockito.doReturn(infobloxClient).when(infoBloxService).buildInfobloxClient(Mockito.any());
      Mockito.doReturn(new ResponseEntity<Void>(HttpStatus.OK)).when(wormholeAPIClient).createHostnameIPMapping(Mockito.any());
   }

   @Test
   public void testExecuteAsyncInvokeIPv4() {
      EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.InfoBlox, null, "10.161.71.154");
      ResponseEntity<FacilitySoftwareConfig[]> infobloxFacilitySoftware = this.getInfobloxFacilitySoftware();
      Mockito.doReturn(new ArrayList<>()).when(infobloxClient).queryHostRecordByIP(Mockito.anyString());
      Mockito.doReturn(this.getInfoBloxIPInfoResults()).when(infobloxClient).queryIpv4addressByIP(Mockito.anyString());
      Mockito.doReturn(new ResponseEntity<>(new Asset(), HttpStatus.OK)).when(wormholeAPIClient).getAssetByName(Mockito.anyString());
      Mockito.doReturn(infobloxFacilitySoftware).when(wormholeAPIClient).getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.InfoBlox);
      Mockito.doReturn(new ResponseEntity<>(new AssetIPMapping[0], HttpStatus.OK)).when(wormholeAPIClient).getHostnameIPMappingByIP(Mockito.anyString());
      infoBloxService.executeAsync(eventMessage);
      TestCase.assertEquals(IntegrationStatus.Status.WARNING, Objects.requireNonNull(infobloxFacilitySoftware.getBody())[0].getIntegrationStatus().getStatus());
   }

   @Test
   public void testExecuteAsyncInvokeRecordHost() {
      EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.InfoBlox, null, "10.161.71.154");
      ResponseEntity<FacilitySoftwareConfig[]> infobloxFacilitySoftware = this.getInfobloxFacilitySoftware();
      Mockito.doReturn(this.getInfoBloxIPInfoResults()).when(infobloxClient).queryHostRecordByIP(Mockito.anyString());
      Mockito.doReturn(new ResponseEntity<>(new Asset(), HttpStatus.OK)).when(wormholeAPIClient).getAssetByName(Mockito.anyString());
      Mockito.doReturn(infobloxFacilitySoftware).when(wormholeAPIClient).getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.InfoBlox);
      Mockito.doReturn(new ResponseEntity<>(new AssetIPMapping[0], HttpStatus.OK)).when(wormholeAPIClient).getHostnameIPMappingByIP(Mockito.anyString());
      infoBloxService.executeAsync(eventMessage);
      TestCase.assertEquals(IntegrationStatus.Status.ACTIVE, Objects.requireNonNull(infobloxFacilitySoftware.getBody())[0].getIntegrationStatus().getStatus());
   }

   @Test
   public void testExecuteAsyncNotFindIP() {
      EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.InfoBlox, null, "10.161.71.154");
      ResponseEntity<FacilitySoftwareConfig[]> infobloxFacilitySoftware = this.getInfobloxFacilitySoftware();
      Mockito.doReturn(infobloxFacilitySoftware).when(wormholeAPIClient).getFacilitySoftwareInternalByType(FacilitySoftwareConfig.SoftwareType.InfoBlox);
      Mockito.doReturn(new ArrayList<>()).when(infobloxClient).queryHostRecordByIP(Mockito.anyString());
      Mockito.doReturn(null).when(infobloxClient).queryIpv4addressByIP(Mockito.anyString());
      infoBloxService.executeAsync(eventMessage);
      TestCase.assertEquals(IntegrationStatus.Status.ACTIVE, Objects.requireNonNull(infobloxFacilitySoftware.getBody())[0].getIntegrationStatus().getStatus());
   }

   private ResponseEntity<FacilitySoftwareConfig[]> getInfobloxFacilitySoftware() {
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
      return new ResponseEntity<>(facilitySoftwareConfigs, HttpStatus.OK);
   }

   private List<InfoBloxIPInfoResult> getInfoBloxIPInfoResults() {
      List<InfoBloxIPInfoResult> expectedResult = new ArrayList<>();
      InfoBloxIPInfoResult infoBloxIPInfoResult1 = new InfoBloxIPInfoResult();
      infoBloxIPInfoResult1.setIpAddress("10.161.71.154");
      infoBloxIPInfoResult1.setHostName("ubuntu01");
      infoBloxIPInfoResult1.setMacAddress("00:50:56:be:60:62");
      expectedResult.add(infoBloxIPInfoResult1);
      return expectedResult;
   }

}
