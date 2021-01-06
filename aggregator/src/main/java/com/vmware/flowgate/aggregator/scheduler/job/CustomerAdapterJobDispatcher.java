/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.aggregator.scheduler.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.model.AdapterJobCommand;
import com.vmware.flowgate.common.model.FacilityAdapter;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.jobs.BaseJob;

@Service
public class CustomerAdapterJobDispatcher extends BaseJob implements Job {

   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;

   @Autowired
   private StringRedisTemplate template;

   @Autowired
   private MessagePublisher publisher;

   private static final int EVERY_CYCLE = 5;
   private static final String NODIFY_MESSAGE = "Nodify message";
   private static final Logger logger = LoggerFactory.getLogger(CustomerAdapterJobDispatcher.class);

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      Map<String,FacilityAdapter> facilityAdapterMap = initFacilityAdapterMap();
      if(facilityAdapterMap.isEmpty()) {
         return;
      }
      List<FacilitySoftwareConfig> facilityIntegrations = getIntegrations();
      if(facilityIntegrations.isEmpty()) {
         return;
      }
      String execountString = template.opsForValue().get(EventMessageUtil.CUSTOMER_ADAPTER_EXECOUNT);
      if (execountString == null || "".equals(execountString)) {
         execountString = "0";
      }
      long execount = Long.valueOf(execountString);
      execount++;
      template.opsForValue().set(EventMessageUtil.CUSTOMER_ADAPTER_EXECOUNT, String.valueOf(execount));
      for (FacilitySoftwareConfig integration : facilityIntegrations) {
         String subcategory = integration.getSubCategory();
         FacilityAdapter adapter = facilityAdapterMap.get(subcategory);
         sendMessage(execount,integration,adapter);
      }
      logger.info("Send customer adapter job finished");
   }

   private Map<String,FacilityAdapter> initFacilityAdapterMap() {
      Map<String,FacilityAdapter> facilityAdapterMap = new HashMap<String,FacilityAdapter>();
      FacilityAdapter[] customerAdapters = restClient.getAllCustomerFacilityAdapters().getBody();
      for(FacilityAdapter adapter : customerAdapters) {
         facilityAdapterMap.put(adapter.getSubCategory(), adapter);
      }
      return facilityAdapterMap;
   }

   private List<FacilitySoftwareConfig> getIntegrations(){
      FacilitySoftwareConfig[] customer_cmdbs = restClient.getFacilitySoftwareInternalByType(SoftwareType.OtherCMDB).getBody();
      FacilitySoftwareConfig[] customer_dcims = restClient.getFacilitySoftwareInternalByType(SoftwareType.OtherDCIM).getBody();
      List<FacilitySoftwareConfig> facilityIntegrations = new ArrayList<FacilitySoftwareConfig>();
      if(customer_cmdbs != null && customer_cmdbs.length != 0) {
         facilityIntegrations.addAll(Arrays.asList(customer_cmdbs));
      }
      if(customer_dcims != null && customer_dcims.length != 0) {
         facilityIntegrations.addAll(Arrays.asList(customer_dcims));
      }
      return facilityIntegrations;
   }

   private void sendMessage(long execount, FacilitySoftwareConfig integration,
         FacilityAdapter adapter) {
      String topic = adapter.getTopic();
      String queue = adapter.getQueueName();
      List<AdapterJobCommand> commands = adapter.getCommands();
      for (AdapterJobCommand command : commands) {
         int executeCycle = command.getTriggerCycle() / EVERY_CYCLE;
         if (execount % executeCycle == 0) {
            try {
               template.opsForList().leftPushAll(queue,
                     EventMessageUtil.generateFacilityMessageListByType(null, command.getCommand(),
                           new FacilitySoftwareConfig[] { integration }));
               EventMessage message =
                     EventMessageUtil.createEventMessage(null, null, NODIFY_MESSAGE);
               publisher.publish(topic, EventMessageUtil.convertEventMessageAsString(message));
               logger.info("Send " + command.getCommand() + " of "+ integration.getName() + " success");
            } catch (IOException e) {
               logger.error("Failed to send out message", e);
            }
         }
      }
   }
}
