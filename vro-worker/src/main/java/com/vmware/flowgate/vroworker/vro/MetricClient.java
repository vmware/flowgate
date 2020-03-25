/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker.vro;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.ops.api.client.controllers.ResourcesClient;
import com.vmware.ops.api.model.property.PropertyContent;
import com.vmware.ops.api.model.property.PropertyContents;
import com.vmware.ops.api.model.resource.ResourceDto;
import com.vmware.ops.api.model.resource.ResourceQuery;
import com.vmware.ops.api.model.stat.StatContent;
import com.vmware.ops.api.model.stat.StatContents;

public class MetricClient extends VROBase {
   private ResourcesClient resourcesClient = null;

   private MessagePublisher publisher;
   public static final List<String> properties = Arrays.asList(VROConsts.LOCATION_BUILDING,
         VROConsts.LOCATION_CABINET, VROConsts.LOCATION_CABINET_NUMBER, VROConsts.LOCATION_CITY,
         VROConsts.LOCATION_COUNTRY, VROConsts.LOCATION_FLOOR, VROConsts.LOCATION_REGION,
         VROConsts.LOCATION_ROOM,"Summary|Location");
   @Override
   public void run() {
      // TODO Auto-generated method stub
      checkPredefinedMetricsandProperties();
   }

   public MetricClient(VROConfig config, MessagePublisher publisher) {
      super(config);
      resourcesClient = getClient().resourcesClient();
      this.publisher = publisher;
   }

   public void checkPredefinedMetricsandProperties() {
      List<String> metrics = Arrays.asList(VROConsts.ENVRIONMENT_PDU_AMPS_METRIC,
            VROConsts.ENVRIONMENT_PDU_VOLTS_METRIC,
            VROConsts.ENVRIONMENT_PDU_AMPS_LOAD_METRIC,
            VROConsts.ENVRIONMENT_PDU_POWER_LOAD_METRIC,
            VROConsts.ENVRIONMENT_PDU_POWER_METRIC,
            VROConsts.ENVRIONMENT_HOSTSENSOR_TEMPERATURE_METRIC,
            VROConsts.ENVRIONMENT_FRONT_TEMPERATURE_METRIC,
            VROConsts.ENVRIONMENT_FRONT_HUMIDITY_METRIC,
            VROConsts.ENVRIONMENT_BACK_HUMIDITY_METRIC,
            VROConsts.ENVRIONMENT_BACK_TEMPERATURE_METRIC);


      List<ResourceDto> hostResources = getHostSystemsResources();
      long timestampAnchor=1428487200;
      for (ResourceDto rd : hostResources) {
         UUID id = rd.getIdentifier();
         StatContents contents = new StatContents();
         for(String metric:metrics) {
            StatContent content = new StatContent();
            content.setStatKey(metric);
            content.setData(new double[] {18.0});
            content.setTimestamps(new long[] {timestampAnchor});//
            contents.addStatContent(content);
         }
         addStats(null, id, contents, false);

         PropertyContents pContents = new PropertyContents();
         for(String property:properties) {
            PropertyContent propertyContent = new PropertyContent();
            propertyContent.setStatKey(property);
            propertyContent.setTimestamps(new long[] {timestampAnchor});
            propertyContent.setValues(new String[] {"unknown"});
            pContents.addPropertyContent(propertyContent);
         }
         addProperties(null, id, pContents);
      }
   }

   /**
    * Push stat data for the specified Stat Keys
    *
    * @param adapterSourceId
    *           the ID of the adapter kind that will push the stats, may be null which defaults to
    *           SuiteAPI adapter
    * @param resourceUUID
    *           vRealize Operations Manager UUID of the Resource
    * @param statKey
    *           Name of the Stat Key 148
    * @param timestamps
    *           Array of long values as timestamps
    * @param data
    *           Array of double values as data
    * @param storeOnly
    *           true if we want to store the data only, false if we want analytics processing to be
    *           performed
    */
   public void addStats(String adapterSourceId, UUID resourceUUID, String statKey,
         long[] timestamps, double[] data, String[] values, boolean storeOnly) {
      StatContents contents = new StatContents();
      StatContent content = new StatContent();
      content.setStatKey(statKey);
      content.setData(data);
      content.setValues(values);
      content.setTimestamps(timestamps);
      contents.getStatContents().add(content);
      if (adapterSourceId == null) {
         resourcesClient.addStats(resourceUUID, contents, false);
      } else {
         resourcesClient.addStats(adapterSourceId, resourceUUID, contents, storeOnly);
      }
   }

   /**
    * Push stat data for the specified Stat Keys
    *
    * @param adapterSourceId
    *           the ID of the adapter kind that will push the stats, may be null which defaults to
    *           SuiteAPI adapter
    * @param resourceUUID
    *           vRealize Operations Manager UUID of the Resource
    * @param statKey
    *           Name of the Stat Key 148
    * @param timestamps
    *           Array of long values as timestamps
    * @param data
    *           Array of double values as data
    * @param storeOnly
    *           true if we want to store the data only, false if we want analytics processing to be
    *           performed
    */
   public void addStats(String adapterSourceId, UUID resourceUUID, StatContents contents,
         boolean storeOnly) {
      if (adapterSourceId == null) {
         resourcesClient.addStats(resourceUUID, contents, false);
      } else {
         resourcesClient.addStats(adapterSourceId, resourceUUID, contents, storeOnly);
      }
   }

   /**
    * Push stat data for the specified Stat Keys
    *
    * @param adapterSourceId
    *           the ID of the adapter kind that will push the stats, may be null which defaults to
    *           SuiteAPI adapter
    * @param resourceUUID
    *           vRealize Operations Manager UUID of the Resource
    * @param statKey
    *           Name of the Stat Key
    * @param timestamps
    *           Array of long values as timestamps
    * @param data
    *           Array of double values as data
    */
   public void addProperties(String adapterSourceId, UUID resourceUUID, String statKey,
         long[] timestamps, double[] data, String[] values) {
      PropertyContents contents = new PropertyContents();
      PropertyContent content = new PropertyContent();
      content.setStatKey(statKey);
      content.setData(data);
      content.setValues(values);
      content.setTimestamps(timestamps);
      contents.getPropertyContents().add(content);
      if (adapterSourceId == null) {
         resourcesClient.addProperties(resourceUUID, contents);
      } else {
         resourcesClient.addProperties(adapterSourceId, resourceUUID, contents);
      }
   }

   /**
    * Push stat data for the specified Stat Keys
    *
    * @param adapterSourceId
    *           the ID of the adapter kind that will push the stats, may be null which defaults to
    *           SuiteAPI adapter
    * @param resourceUUID
    *           vRealize Operations Manager UUID of the Resource
    * @param contents
    *           A list of PropertyContent
    */
   public void addProperties(String adapterSourceId, UUID resourceUUID, PropertyContents contents) {
      if (adapterSourceId == null) {
         resourcesClient.addProperties(resourceUUID, contents);
      } else {
         resourcesClient.addProperties(adapterSourceId, resourceUUID, contents);
      }
   }

   public List<ResourceDto> getHostSystemsResources() {
      ResourceQuery rq = new ResourceQuery();
      rq.setAdapterKind(new String[] { VROConsts.ADPTERKIND_VMARE_KEY });
      rq.setResourceKind(new String[] { VROConsts.RESOURCEKIND_HOSTSYSTEM_KEY });
      List<ResourceDto> resources = resourcesClient.getResources(rq, null).getResourceList();
      return resources;
   }

   //   public static void main(String[]args) {
   //      new MetricClient(null).run();
   //   }

}
