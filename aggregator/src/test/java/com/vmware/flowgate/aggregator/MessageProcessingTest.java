/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.aggregator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.aggregator.scheduler.job.AggregatorService;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageImpl;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;

import junit.framework.TestCase;

public class MessageProcessingTest {

   @Autowired
   private StringRedisTemplate template;

   @Spy
   @InjectMocks
   private AggregatorService aggregatorService = new AggregatorService();


   @Test
   public void testRemovePduFromServer() {
      Asset[] servers = new Asset[2];
      Asset server = new Asset();
      List<String> pduids = new ArrayList<String>();
      pduids.add("qwiounasdyi2avewrasdf");
      pduids.add("mienoas2389asddsfqzda");
      server.setPdus(pduids);
      HashMap<String,String> justficationfields = new HashMap<String,String>();
      String pduPorts = "01"+FlowgateConstant.SEPARATOR+"pdu1"+FlowgateConstant.SEPARATOR+"01"+FlowgateConstant.SEPARATOR+"qwepouasdnlksaydasmnd"+FlowgateConstant.SPILIT_FLAG
            + "02"+FlowgateConstant.SEPARATOR+"pdu2"+FlowgateConstant.SEPARATOR+"02"+FlowgateConstant.SEPARATOR+"mienoas2389asddsfqzda";
      justficationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, pduPorts);
      server.setJustificationfields(justficationfields);
      Map<String,Map<String,Map<String,String>>> metricsFormular =
            new HashMap<String,Map<String,Map<String,String>>>();
      Map<String,Map<String,String>> pduMetrics = new HashMap<String,Map<String,String>>();
      Map<String,String> pduinfo1 = new HashMap<String,String>();
      pduinfo1.put(MetricName.PDU_ACTIVE_POWER, "mienoas2389asddsfqzda");
      pduMetrics.put("mienoas2389asddsfqzda", pduinfo1);

      Map<String,String> pduinfo2 = new HashMap<String,String>();
      pduinfo2.put(MetricName.PDU_ACTIVE_POWER, "asdasdw2213dsdfaewwqe");
      pduMetrics.put("asdasdw2213dsdfaewwqe", pduinfo2);
      metricsFormular.put(FlowgateConstant.PDU, pduMetrics);
      server.setMetricsformulars(metricsFormular);
      servers[0] = server;

      Asset server1 = new Asset();
      List<String> pduids1 = new ArrayList<String>();
      pduids1.add("asdasdasd");
      pduids1.add("qweertwtc");
      server1.setPdus(pduids1);
      servers[1] = server1;

      HashSet<String> pduAssetIDs = new HashSet<String>();
      pduAssetIDs.add("mienoas2389asddsfqzda");
      pduAssetIDs.add("asdw2cvxcjchftyhretyv");
      List<Asset> needToUpdateServer = aggregatorService.removePduFromServer(servers, pduAssetIDs);
      TestCase.assertEquals(1, needToUpdateServer.size());
      Asset needupdateServer = needToUpdateServer.get(0);
      TestCase.assertEquals("01"+FlowgateConstant.SEPARATOR+"pdu1"+FlowgateConstant.SEPARATOR+"01"+FlowgateConstant.SEPARATOR+"qwepouasdnlksaydasmnd",
            needupdateServer.getJustificationfields().get(FlowgateConstant.PDU_PORT_FOR_SERVER));
      TestCase.assertEquals(1,needupdateServer.getPdus().size());
      TestCase.assertEquals("qwiounasdyi2avewrasdf", needupdateServer.getPdus().get(0));
      Map<String,Map<String,Map<String,String>>> metricsFormulars =
            needupdateServer.getMetricsformulars();
      TestCase.assertEquals(1, metricsFormulars.size());
      TestCase.assertEquals("asdasdw2213dsdfaewwqe", metricsFormulars.get(FlowgateConstant.PDU).keySet().iterator().next());
   }
}
