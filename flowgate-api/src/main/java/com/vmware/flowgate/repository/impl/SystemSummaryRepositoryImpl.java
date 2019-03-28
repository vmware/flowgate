/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.BasicDBObject;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.NlyteSummary;
import com.vmware.flowgate.common.model.PowerIqSummary;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.SystemSummary;
import com.vmware.flowgate.common.model.VcSummary;
import com.vmware.flowgate.common.model.VroSummary;
import com.vmware.flowgate.repository.SystemSummaryRepositoryEnhancement;

/**
 * @author haoyul
 *
 */
public class SystemSummaryRepositoryImpl implements SystemSummaryRepositoryEnhancement {

   /* (non-Javadoc)
    * @see com.vmware.wormhole.repository.SystemSummaryRepositoryEnhancement#getSystemSummaryData()
    */
   @Autowired
   MongoTemplate mongoTemplate;

   @Override
   public SystemSummary getSystemSummaryData() {
      SystemSummary data = new SystemSummary();
      data.setAssetsNum(getAggregationDataNumber("_class", "com.vmware.wormhole.common.model.Asset",
            "_class", "asset"));
      data.setFacilitySystemNum(getAggregationDataNumber("_class",
            "com.vmware.wormhole.common.model.FacilitySoftwareConfig", "_class",
            "facilitySoftwareConfig"));
      data.setServerNum(getAggregationDataNumber("category", AssetCategory.Server.toString(),
            "category", "asset"));
      data.setPduNum(getAggregationDataNumber("category", AssetCategory.PDU.toString(), "category",
            "asset"));
      data.setSensorNum(getAggregationDataNumber("category", AssetCategory.Sensors.toString(),
            "category", "asset"));
      data.setCabinetNum(getAggregationDataNumber("category", AssetCategory.Cabinet.toString(),
            "category", "asset"));
      data.setSwitchNum(getAggregationDataNumber("category", AssetCategory.Networks.toString(),
            "category", "asset"));
      data.setUserNum(getAggregationDataNumber("_class",
            "com.vmware.wormhole.common.model.WormholeUser", "_class", "wormholeUser"));
      data.setSddcServerNum(getAggregationDataNumber("_class",
            "com.vmware.wormhole.common.model.ServerMapping", "_class", "serverMapping"));
      data.setSddcIntegrationNum(getAggregationDataNumber("_class",
            "com.vmware.wormhole.common.model.SDDCSoftwareConfig", "_class", "sDDCSoftwareConfig"));
      data.setVcNum(getAggregationDataNumber("type", "VCENTER", "type", "sDDCSoftwareConfig"));
      data.setVroNum(getAggregationDataNumber("type", "VRO", "type", "sDDCSoftwareConfig"));
      data.setAirFlowSensorNum(getAggregationDataNumber("subCategory",
            AssetSubCategory.AirFlow.toString(), "subCategory", "asset"));
      data.setHumiditySensorNum(getAggregationDataNumber("subCategory",
            AssetSubCategory.Humidity.toString(), "subCategory", "asset"));
      data.setSmokeSensorNum(getAggregationDataNumber("subCategory",
            AssetSubCategory.Smoke.toString(), "subCategory", "asset"));
      data.setTemperatureSensorNum(getAggregationDataNumber("subCategory",
            AssetSubCategory.Temperature.toString(), "subCategory", "asset"));
      data.setWaterSensorNum(getAggregationDataNumber("subCategory",
            AssetSubCategory.Water.toString(), "subCategory", "asset"));
      data.setNlyteSummary(getNlyteSummary());
      data.setPowerIqSummary(getPowerIqSummary());
      data.setVcSummary(getVcSummary());
      data.setVroSummary(getVroSummary());
      return data;
   }

   public List<VcSummary> getVcSummary() {
      List<VcSummary> vcSummary = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("VCENTER"));
      List<SDDCSoftwareConfig> sddcSoftwareConfig =
            mongoTemplate.find(query, SDDCSoftwareConfig.class);
      for (SDDCSoftwareConfig s : sddcSoftwareConfig) {
         VcSummary vc = new VcSummary();
         vc.setName(s.getName());
         vc.setUrl(s.getServerURL());
         vc.setHostsNum(getAggregationServerDetail(s.getId(), "vcID"));
         vcSummary.add(vc);
      }
      return vcSummary;
   }

   public List<VroSummary> getVroSummary() {
      List<VroSummary> vroSummary = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("VRO"));
      List<SDDCSoftwareConfig> sddcSoftwareConfig =
            mongoTemplate.find(query, SDDCSoftwareConfig.class);
      for (SDDCSoftwareConfig s : sddcSoftwareConfig) {
         VroSummary vro = new VroSummary();
         vro.setName(s.getName());
         vro.setUrl(s.getServerURL());
         vro.setHostsNum(getAggregationServerDetail(s.getId(), "vroID"));
         vroSummary.add(vro);
      }
      return vroSummary;
   }

   public List<NlyteSummary> getNlyteSummary() {
      List<NlyteSummary> nlyteSummary = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("Nlyte"));
      List<FacilitySoftwareConfig> facility =
            mongoTemplate.find(query, FacilitySoftwareConfig.class);
      for (FacilitySoftwareConfig f : facility) {
         NlyteSummary nlyte = new NlyteSummary();
         nlyte.setName(f.getName());
         nlyte.setUrl(f.getServerURL());
         nlyte.setSensorNum(
               getAggregationSystemDetail(f.getId(), AssetCategory.Sensors.toString()));
         nlyte.setServerNum(getAggregationSystemDetail(f.getId(), AssetCategory.Server.toString()));
         nlyte.setSwitchNum(
               getAggregationSystemDetail(f.getId(), AssetCategory.Networks.toString()));
         nlyte.setPduNum(getAggregationSystemDetail(f.getId(), AssetCategory.PDU.toString()));
         nlyte.setCabinetNum(
               getAggregationSystemDetail(f.getId(), AssetCategory.Cabinet.toString()));
         nlyteSummary.add(nlyte);
      }
      return nlyteSummary;
   }

   public List<PowerIqSummary> getPowerIqSummary() {
      List<PowerIqSummary> powerIqSummary = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("PowerIQ"));
      List<FacilitySoftwareConfig> facility =
            mongoTemplate.find(query, FacilitySoftwareConfig.class);
      for (FacilitySoftwareConfig f : facility) {
         PowerIqSummary powerIq = new PowerIqSummary();
         powerIq.setName(f.getName());
         powerIq.setUrl(f.getServerURL());
         powerIq.setSensorNum(
               getAggregationSystemDetail(f.getId(), AssetCategory.Sensors.toString()));
         powerIqSummary.add(powerIq);
      }
      return powerIqSummary;
   }

   public int getAggregationSystemDetail(String assetSource, String category) {
      int num = 0;
      Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("assetSource").is(assetSource)),
            Aggregation.match(Criteria.where("category").is(category)),
            Aggregation.group("_class").count().as("sum"),
            Aggregation.match(Criteria.where("sum").gte(0)));

      AggregationResults<BasicDBObject> res =
            mongoTemplate.aggregate(aggregation, "asset", BasicDBObject.class);
      if (!res.getMappedResults().isEmpty()) {
         num = (int) res.getUniqueMappedResult().get("sum");
      }
      return num;
   }

   public int getAggregationServerDetail(String id, String type) {
      int num = 0;
      Aggregation aggregation =
            Aggregation.newAggregation(Aggregation.match(Criteria.where(type).is(id)),
                  Aggregation.group(type).count().as("sum"),
                  Aggregation.match(Criteria.where("sum").gte(0)));

      AggregationResults<BasicDBObject> res =
            mongoTemplate.aggregate(aggregation, "serverMapping", BasicDBObject.class);
      if (!res.getMappedResults().isEmpty()) {
         num = (int) res.getUniqueMappedResult().get("sum");
      }
      return num;
   }

   public int getAggregationDataNumber(String where, String is, String group, String dataBase) {
      int num = 0;
      Aggregation aggregation =
            Aggregation.newAggregation(Aggregation.match(Criteria.where(where).is(is)),
                  Aggregation.group(group).count().as("sum"),
                  Aggregation.match(Criteria.where("sum").gte(0)));

      AggregationResults<BasicDBObject> res =
            mongoTemplate.aggregate(aggregation, dataBase, BasicDBObject.class);
      if (!res.getMappedResults().isEmpty()) {
         num = (int) res.getUniqueMappedResult().get("sum");
      }
      return num;
   }

}
