/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model.redis.message.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventSource;
import com.vmware.flowgate.common.model.redis.message.EventTarget;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.EventUser;

public class EventMessageUtil {
   private static ObjectMapper mapper = new ObjectMapper();


   public static final String AggregatorTopic = "aggregator";
   public static final String InfobloxTopic = "infoblox";
   public static final String VCTopic = "vctopic";
   public static final String VROTopic = "vrotopic";
   public static final String POWERIQTopic = "poweriqtopic";
   public static final String NLYTETOPIC = "nlytetopic";
   public static final String LabsdbTopic = "labsdbtopic";
   public static final String OpenManageTopic = "openmanagetopic";

   public static final String powerIQJobList = "poweriqjobList";
   public static final String nlyteJobList = "nlytejobList";
   public static final String vcJobList = "vcjobList";
   public static final String vroJobList = "vrojobList";
   public static final String labsdbJobList = "labsdbjoblist";
   public static final String OpenManageJobList = "openmanagejoblist";
   public static final String HostNameIPMappingCommand = "hostnameipmapping";
   public static final String FullMappingCommand = "fullservermapping";
   public static final String PDUServerMappingCommand = "pduservermapping";
   public static final String SyncTemperatureAndHumiditySensors = "tempandhumiditymapping";
   public static final String FullSyncTemperatureAndHumiditySensors = "fullsynctempandhumiditymapping";
   public static final String CleanRealtimeData = "cleanRealtimeData";
   public static final String AggregateAndCleanPowerIQPDU = "aggregateandcleanpoweriqpdu";
   public static final String SUMMARY_DATA = "summarydata";

   public static final String AGGREGATOR_EXECOUNT = "aggregator.execount";

   public static final String VCENTER_SyncCustomerAttrs = "vcenter.synccustomerattrs";
   public static final String VCENTER_SyncCustomerAttrsData = "vcenter.synccustomerattrsdata";
   public static final String VCENTER_SyncData = "vcenter.syncdata";
   public static final String VCENTER_EXECOUNT = "vcenter.execount";

   public static final String VCENTER_QueryHostMetaData = "vcenter.queryhostmetadata";
   public static final String VCENTER_QueryHostUsageData = "vcenter.queryhostusagedata";

   public static final String VRO_SyncData = "vro.syncdata";
   public static final String VRO_SyncMetricData = "vro.syncmetricdata";
   public static final String VRO_SyncMetricPropertyAndAlert = "vro.syncmetricpropertyalert";
   public static final String VRO_EXECOUNT = "vro.execount";

   public static final String NLYTE_SyncData= "nlyte.syncdata";
   public static final String NLYTE_SyncAllAssets = "nlyte.syncallassets";
   public static final String NLYTE_SyncRealtimeData="nlyte.syncrealtimedata";
   public static final String NLYTE_SyncMappedAssetData="nlyte.syncmappedassets";
   public static final String NLYTE_CleanInActiveAssetData="nlyte.cleaninactiveassetdata";
   public static final String NLYTE_EXECOUNT = "nlyte.execount";

   public static final String PowerIQ_SyncRealtimeData="poweriq.syncrealtimedata";
   public static final String PowerIQ_SyncAssetsMetaData="poweriq.syncassetsmetadata";
   public static final String POWERIQ_SyncData= "poweriq.syncdata";
   public static final String PowerIQ_SyncAllPDUID = "poweriq.syncallpduid";
   public static final String PowerIQ_SyncAllSensorMetricFormula = "poweriq.syncallsensormetricformula";
   public static final String PowerIQ_CleanInActiveAssetData="poweriq.cleaninactiveassetdata";
   public static final String POWERIQ_EXECOUNT = "poweriq.execount";

   public static final String Labsdb_SyncAllWireMapData = "labsdb.syncallwiremapdata";
   public static final String Labsdb_SyncUnMappedAssetWiremapData = "labsdb.syncunmappedassetwiremapdata";
   public static final String Labsdb_SyncData = "labsdb.syncdata";
   public static final String LABSDB_EXECOUNT = "labsdb.execount";

   public static final String OpenManage_SyncData= "openmanage.syncdata";
   public static final String OpenManage_SyncRealtimeData="openmanage.syncrealtimedata";
   public static final String OpenManage_SyncAssetsMetaData="openmanage.syncassetsmetadata";
   public static final String OpenManage_EXECOUNT = "openmanage.execount";

   public static String EXPIREDTIMERANGE = "EXPIREDTIMERANGE";
   public static String CUSTOMER_ADAPTER_EXECOUNT = "customerAdapter.execount";

   private static Map<EventType, String> typeAndCommandIdMap = new HashMap<EventType, String>();
   static {
      typeAndCommandIdMap.put(EventType.Nlyte, EventMessageUtil.NLYTE_SyncData);
      typeAndCommandIdMap.put(EventType.PowerIQ, EventMessageUtil.POWERIQ_SyncData);
      typeAndCommandIdMap.put(EventType.Labsdb, EventMessageUtil.Labsdb_SyncData);
      typeAndCommandIdMap.put(EventType.OpenManage, EventMessageUtil.OpenManage_SyncData);
      typeAndCommandIdMap = Collections.unmodifiableMap(typeAndCommandIdMap);
   }
   public static EventMessage convertToEventMessage(EventType type, String message) {
      EventMessage eventMessage =
            new EventMessageImpl(type, null, null, null, new Date().getTime(), message);
      return eventMessage;
   }

   public static String convertToEventMessageAsString(EventType type, String message)
         throws IOException {
      return mapper.writeValueAsString(convertToEventMessage(type, message));
   }

   public static EventMessage convertToEventMessage(EventType type, EventUser user,
         EventSource source, EventTarget target, String message) {
      EventMessage eventMessage =
            new EventMessageImpl(type, user, source, target, new Date().getTime(), message);
      return eventMessage;
   }

   public static String convertEventMessageAsString(EventMessage message) throws IOException {
      return mapper.writeValueAsString(message);
   }

   public static EventMessage createEventMessage(EventType type, String targetCommand,
         String message) {
      EventUser targetUser = new EventUserImpl(targetCommand);

      Set<EventUser> users = new HashSet<EventUser>();
      users.add(targetUser);
      EventTarget target = new EventTargetImpl(users);
      EventMessage newMessage =
            new EventMessageImpl(type, null, null, target, new Date().getTime(), message);
      return newMessage;
   }

   public static List<String> generateSDDCMessageListByType(EventType type, String targetCommand,
         SDDCSoftwareConfig[] sddcs) throws JsonProcessingException {
      List<String> result = new ArrayList<String>();
      for (SDDCSoftwareConfig sddc : sddcs) {
         String payload = mapper.writeValueAsString(sddc);
         EventMessage message = EventMessageUtil.createEventMessage(type, targetCommand, payload);
         result.add(mapper.writeValueAsString(message));
      }
      return result;
   }
   public static List<String> generateFacilityMessageListByType(EventType type, String targetCommand,
         FacilitySoftwareConfig[] facilites) throws JsonProcessingException {
      List<String> result = new ArrayList<String>();
      for (FacilitySoftwareConfig facility : facilites) {
         String payload = mapper.writeValueAsString(facility);
         EventMessage message = EventMessageUtil.createEventMessage(type, targetCommand, payload);
         result.add(mapper.writeValueAsString(message));
      }
      return result;
   }

   public static String generateSDDCNotifyMessage(EventType type) throws IOException {
      EventMessage message = null;
      switch (type) {
      case VCenter:
         message = EventMessageUtil.createEventMessage(type, EventMessageUtil.VCENTER_SyncData, "");
         break;
      case VROps:
         message = EventMessageUtil.createEventMessage(type, EventMessageUtil.VRO_SyncData, "");
         break;
      default:
         return null;
      }
      return EventMessageUtil.convertEventMessageAsString(message);
   }

   public static String generateFacilityNotifyMessage(EventType type) throws IOException {
      String command = typeAndCommandIdMap.get(type);
      if(command == null) {
         return null;
      }
      EventMessage message = EventMessageUtil.createEventMessage(type, command, "");;
      return EventMessageUtil.convertEventMessageAsString(message);
   }
}
