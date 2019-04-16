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
    * @see com.vmware.flowgate.repository.SystemSummaryRepositoryEnhancement#getSystemSummaryData()
    */
   @Autowired
   MongoTemplate mongoTemplate;
   
   @Override
   public SystemSummary getSystemResult() {
      SystemSummary data = new SystemSummary();
      data.setAssetsNum(getCountRes(null, null, "asset"));
      data.setFacilitySystemNum(getCountRes(null, null, "facilitySoftwareConfig"));
      data.setUserNum(getCountRes(null, null, "wormholeUser"));
      data.setSddcServerNum(getCountRes(null, null, "serverMapping"));
      data.setSddcIntegrationNum(getCountRes(null, null, "sDDCSoftwareConfig"));
      data.setVcNum(getCountRes("type", "VCENTER", "sDDCSoftwareConfig"));
      data.setVroNum(getCountRes("type", "VRO", "sDDCSoftwareConfig"));
      data = getAssetNumGroupByCategory(data);
      data = getSensorNumGroupBySubCategory(data);
      data.setNlyteSummary(getNlyteSummaryList());
      data.setPowerIqSummary(getPowerIQSummaryList());
      data.setVcSummary(getVcSummaryList());
      data.setVroSummary(getVroSummaryList());
      return data;
   }

   public List<VcSummary> getVcSummaryList() {
      List<VcSummary> vcSummary = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("VCENTER"));
      List<SDDCSoftwareConfig> sddcSoftwareConfig =
            mongoTemplate.find(query, SDDCSoftwareConfig.class);
      for (SDDCSoftwareConfig s : sddcSoftwareConfig) {
         VcSummary vc = new VcSummary();
         vc.setName(s.getName());
         vc.setUrl(s.getServerURL());
         vc.setHostsNum(getCountRes("vcID", s.getId(), "serverMapping"));
         vcSummary.add(vc);
      }
      return vcSummary;
   }

   public List<VroSummary> getVroSummaryList() {
      List<VroSummary> vroSummary = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("VRO"));
      List<SDDCSoftwareConfig> sddcSoftwareConfig =
            mongoTemplate.find(query, SDDCSoftwareConfig.class);
      for (SDDCSoftwareConfig s : sddcSoftwareConfig) {
         VroSummary vro = new VroSummary();
         vro.setName(s.getName());
         vro.setUrl(s.getServerURL());
         vro.setHostsNum(getCountRes("vroID", s.getId(), "serverMapping"));
         vroSummary.add(vro);
      }
      return vroSummary;
   }

   public List<PowerIqSummary> getPowerIQSummaryList() {
      List<PowerIqSummary> powerIqSummarys = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("PowerIQ"));
      List<FacilitySoftwareConfig> facility =
            mongoTemplate.find(query, FacilitySoftwareConfig.class);
      for (FacilitySoftwareConfig powerIq : facility) {
         PowerIqSummary powerIQSummary = getPowerIQSummary(powerIq);
         powerIqSummarys.add(powerIQSummary);
      }
      return powerIqSummarys;
   }
   
   public PowerIqSummary getPowerIQSummary(FacilitySoftwareConfig powerIQ) {
      PowerIqSummary powerIQSummary = new PowerIqSummary();
      powerIQSummary.setName(powerIQ.getName());
      powerIQSummary.setUrl(powerIQ.getServerURL());
      Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("assetSource").is(powerIQ.getId())),
            Aggregation.group("category").count().as("num"),
            Aggregation.match(Criteria.where("num").gt(0)));

      AggregationResults<BasicDBObject> res =
            mongoTemplate.aggregate(aggregation, "asset", BasicDBObject.class);
      List<BasicDBObject> basicDBObjects  = res.getMappedResults();
      if (!basicDBObjects.isEmpty()) {
         for(BasicDBObject db :basicDBObjects) {
            if(db.get("_id") == null) {
               continue;
            }
            switch (db.get("_id").toString()) {
            case "PDU":
               powerIQSummary.setPduNum(db.getInt("num"));
               break;
            case "Sensors":
               powerIQSummary.setSensorNum(db.getInt("num"));
               break;
            default:
               break;
            }
         }
      }
      return powerIQSummary;
   }
   
   public List<NlyteSummary> getNlyteSummaryList() {
      List<NlyteSummary> nlyteSummarys = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("Nlyte"));
      List<FacilitySoftwareConfig> facility =
            mongoTemplate.find(query, FacilitySoftwareConfig.class);
      for (FacilitySoftwareConfig nlyte : facility) {
         NlyteSummary nlyteSummary = getNlyteSummary(nlyte);
         nlyteSummarys.add(nlyteSummary);
      }
      return nlyteSummarys;
   }
   
   public NlyteSummary getNlyteSummary(FacilitySoftwareConfig nlyte) {
      NlyteSummary nlyteSummary = new NlyteSummary();
      nlyteSummary.setName(nlyte.getName());
      nlyteSummary.setUrl(nlyte.getServerURL());
      Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("assetSource").is(nlyte.getId())),
            Aggregation.group("category").count().as("num"),
            Aggregation.match(Criteria.where("sum").gt(0)));

      AggregationResults<BasicDBObject> res =
            mongoTemplate.aggregate(aggregation, "asset", BasicDBObject.class);
      List<BasicDBObject> basicDBObjects  = res.getMappedResults();
      if (!basicDBObjects.isEmpty()) {
         for(BasicDBObject db :basicDBObjects) {
            if(db.get("_id") == null) {
               continue;
            }
            switch (db.get("_id").toString()) {
            case "Server":
               nlyteSummary.setServerNum(db.getInt("num"));
               break;
            case "PDU":
               nlyteSummary.setPduNum(db.getInt("num"));
               break;
            case "Sensors":
               nlyteSummary.setSensorNum(db.getInt("num"));
               break;
            case "Cabinet":
               nlyteSummary.setCabinetNum(db.getInt("num"));
               break;
            case "Networks":
               nlyteSummary.setSwitchNum(db.getInt("num"));
               break;
            default:
               break;
            }
         }
      }
      return nlyteSummary;
   }
   
   public int getCountRes(String filed,String value,String collectionName) {
      Query query = null;
      if(filed != null) {
         query = new Query();
         query.addCriteria(Criteria.where(filed).is(value));
      }
      Long res = mongoTemplate.count(query, collectionName);
      return res.intValue();
   }
   
   public SystemSummary getAssetNumGroupByCategory(SystemSummary data) {
      Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.group("category").count().as("num"),
            Aggregation.match(Criteria.where("num").gt(0)));

      AggregationResults<BasicDBObject> res =
            mongoTemplate.aggregate(aggregation, "asset", BasicDBObject.class);
      List<BasicDBObject> basicDBObjects = res.getMappedResults();
      if(!basicDBObjects.isEmpty()) {
         for(BasicDBObject db :basicDBObjects) {
            if(db.get("_id") == null) {
               continue;
            }
            switch (db.get("_id").toString()) {
            case "Server":
               data.setServerNum(db.getInt("num"));
               break;
            case "PDU":
               data.setPduNum(db.getInt("num"));
               break;
            case "Sensors":
               data.setSensorNum(db.getInt("num"));
               break;
            case "Cabinet":
               data.setCabinetNum(db.getInt("num"));
               break;
            case "Networks":
               data.setSwitchNum(db.getInt("num"));
               break;
            default:
               break;
            }
         }
      }
      return data;
   }
   
   public SystemSummary getSensorNumGroupBySubCategory(SystemSummary data) {
      Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("category").is("Sensors")),
            Aggregation.group("subCategory").count().as("num"),
            Aggregation.match(Criteria.where("num").gt(0)));

      AggregationResults<BasicDBObject> res =
            mongoTemplate.aggregate(aggregation, "asset", BasicDBObject.class);
      List<BasicDBObject> basicDBObjects = res.getMappedResults();
      if(!basicDBObjects.isEmpty()) {
         for(BasicDBObject db :basicDBObjects) {
            if(db.get("_id") == null) {
               continue;
            }
            switch (db.get("_id").toString()) {
            case "Humidity":
               data.setHumiditySensorNum(db.getInt("num"));
               break;
            case "Temperature":
               data.setTemperatureSensorNum(db.getInt("num"));
               break;
            case "AirFlow":
               data.setAirFlowSensorNum(db.getInt("num"));
               break;
            case "Smoke":
               data.setSmokeSensorNum(db.getInt("num"));
               break;
            case "Water":
               data.setWaterSensorNum(db.getInt("num"));
               break;
            default:
               break;
            }
         }
      }
      return data;
   }
   
}
