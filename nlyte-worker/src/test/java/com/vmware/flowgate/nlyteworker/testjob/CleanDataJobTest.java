/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.testjob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.ServerSensorData;
import com.vmware.flowgate.common.model.ServerSensorData.ServerSensorType;
import com.vmware.flowgate.nlyteworker.config.ServiceKeyConfig;
import com.vmware.flowgate.nlyteworker.restclient.NlyteAPIClient;
import com.vmware.flowgate.nlyteworker.scheduler.job.NlyteDataService;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class CleanDataJobTest {
   @Mock
   private WormholeAPIClient wormholeAPIClient;

   @Mock
   private NlyteAPIClient nlyteAPIClient;

   @Mock
   private ServiceKeyConfig config;

   @Spy
   @InjectMocks
   private NlyteDataService nlyteDataService = new NlyteDataService();


   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
   }

   @Test
   public void removePduFromServerTest() {
      List<Asset> servers = new ArrayList<Asset>();
      Asset server = new Asset();
      List<String> pdus = new ArrayList<String>();
      pdus.add("0364");
      pdus.add("po09");
      server.setPdus(pdus);
      HashMap<String, String> justifications = new HashMap<String, String>();
      justifications.put(FlowgateConstant.PDU_PORT_FOR_SERVER,
            "pci-2:hba:1_FIELDSPLIT_cloud-fc02-sha1_FIELDSPLIT_05_FIELDSPLIT_0364,onboard:1gb-nic:4_FIELDSPLIT_cloud-sw02-sha1_FIELDSPLIT_08_FIELDSPLIT_3fc319e50d21476684d841aa0842bd52");
      server.setJustificationfields(justifications);
      Map<ServerSensorType,String> sensorsformular = new HashMap<ServerSensorData.ServerSensorType, String>();
      sensorsformular.put(ServerSensorType.PDU_RealtimeLoad, "0364+op00+klwd");
      sensorsformular.put(ServerSensorType.PDU_RealtimePower, "0364+op00+klwd");
      sensorsformular.put(ServerSensorType.PDU_RealtimeVoltage, "op00+klwd");
      server.setSensorsformulars(sensorsformular);
      servers.add(server);

      Asset server2 = new Asset();
      List<String> pdusfor2 = new ArrayList<String>();
      pdusfor2.add("oi23");
      pdusfor2.add("sd23");
      server2.setPdus(pdusfor2);
      servers.add(server2);

      servers = nlyteDataService.removePduFromServer(servers, "0364");
      TestCase.assertEquals(1, servers.size());
      TestCase.assertEquals(1, servers.get(0).getPdus().size());
      TestCase.assertEquals(1, servers.get(0).getSensorsformulars().size());
      TestCase.assertEquals("op00+klwd", servers.get(0).getSensorsformulars().get(ServerSensorType.PDU_RealtimeVoltage));
      TestCase.assertEquals("onboard:1gb-nic:4_FIELDSPLIT_cloud-sw02-sha1_FIELDSPLIT_08_FIELDSPLIT_3fc319e50d21476684d841aa0842bd52", servers.get(0).getJustificationfields().get(FlowgateConstant.PDU_PORT_FOR_SERVER));
   }
}
