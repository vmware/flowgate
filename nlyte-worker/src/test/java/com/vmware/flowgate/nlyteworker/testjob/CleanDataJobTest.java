/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.testjob;

import java.util.ArrayList;
import java.util.List;

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
import com.vmware.flowgate.common.model.Asset;
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
   public void updatePduAndSwitchTest() {
      Asset server = new Asset();
      List<String> pdus = new ArrayList<String>();
      pdus.add("0364");
      pdus.add("po09");
      List<String> switches = new ArrayList<String>();
      switches.add("qwe23");
      switches.add("oo09w");
      server.setPdus(pdus);
      server.setSwitches(switches);
      List<String> switches1 = new ArrayList<String>();
      switches1.add("qwe23");
      switches1.add("wertd");
      switches1.add("567uy");
      switches1.add("adwgs");
      server = nlyteDataService.updatePduAndSwitch(server, pdus, switches1);

      TestCase.assertEquals(0, server.getPdus().size());
      TestCase.assertEquals(1, server.getSwitches().size());
   }
}
