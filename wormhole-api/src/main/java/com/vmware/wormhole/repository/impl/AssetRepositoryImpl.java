/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.repository.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.BasicDBObject;
import com.mongodb.WriteResult;
import com.vmware.wormhole.common.AssetCategory;
import com.vmware.wormhole.common.AssetSubCategory;
import com.vmware.wormhole.common.model.Asset;
import com.vmware.wormhole.common.model.DashBoardData;
import com.vmware.wormhole.common.model.DashBoardSystemNlyteDetail;
import com.vmware.wormhole.common.model.DashBoardSystemPowerIqDetail;
import com.vmware.wormhole.common.model.DashBoardSystemSddcServerVcDetail;
import com.vmware.wormhole.common.model.DashBoardSystemSddcServerVroDetail;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.wormhole.common.model.SDDCSoftwareConfig;
import com.vmware.wormhole.repository.AssetRepositoryEnhancement;


public class AssetRepositoryImpl implements AssetRepositoryEnhancement {

   @Autowired
   MongoTemplate mongoTemplate;

   @Override
   public int updateAssetByFileds(String id, HashMap<String, Object> fieldsAndValues) {

      Query query = new Query(Criteria.where("id").is(id));
      Update update = new Update();
      for (String key : fieldsAndValues.keySet()) {
         update.set(key, fieldsAndValues.get(key));
      }

      WriteResult result = mongoTemplate.updateFirst(query, update, Asset.class);

      if (result != null) {
         return result.getN();
      }
      return 0;
   }

   @Override
   public List<Asset> findByIDs(List<String> IDs) {
      Query query = new Query();
      query.addCriteria(Criteria.where("id").in(IDs));
      return mongoTemplate.find(query, Asset.class);
   }

   @Override
   public DashBoardData getAllDashBoardData() {
      DashBoardData data = new DashBoardData();
      data.setAssetsNum(getAggregationDataNumber("_class", "com.vmware.wormhole.common.model.Asset",
            "_class", "asset"));
      data.setFacilitySystemNum(getAggregationDataNumber("_class",
            "com.vmware.wormhole.common.model.FacilitySoftwareConfig", "_class",
            "facilitySoftwareConfig"));
      data.setCategoryIsServerNum(getAggregationDataNumber("category",
            AssetCategory.Server.toString(), "category", "asset"));
      data.setCategoryIsPduNum(getAggregationDataNumber("category", AssetCategory.PDU.toString(),
            "category", "asset"));
      data.setCategoryIsSensorNum(getAggregationDataNumber("category",
            AssetCategory.Sensors.toString(), "category", "asset"));
      data.setCategoryIsCabinetNum(getAggregationDataNumber("category",
            AssetCategory.Cabinet.toString(), "category", "asset"));
      data.setCategoryIsSwitchNum(getAggregationDataNumber("category",
            AssetCategory.Networks.toString(), "category", "asset"));
      data.setCategoryIsUpsNum(getAggregationDataNumber("category", AssetCategory.UPS.toString(),
            "category", "asset"));
      data.setUserNum(getAggregationDataNumber("_class",
            "com.vmware.wormhole.common.model.WormholeUser", "_class", "wormholeUser"));
      data.setSddcServerNum(getAggregationDataNumber("_class",
            "com.vmware.wormhole.common.model.ServerMapping", "_class", "serverMapping"));
      data.setSddcIntegrationNum(getAggregationDataNumber("_class",
            "com.vmware.wormhole.common.model.SDDCSoftwareConfig", "_class", "sDDCSoftwareConfig"));
      data.setSddcIntegrationVcNum(
            getAggregationDataNumber("type", "VCENTER", "type", "sDDCSoftwareConfig"));
      data.setSddcIntegrationVroNum(
            getAggregationDataNumber("type", "VRO", "type", "sDDCSoftwareConfig"));
      data.setSubCategoryIsAirFlow(getAggregationDataNumber("subCategory",
            AssetSubCategory.AirFlow.toString(), "subCategory", "asset"));
      data.setSubCategoryIsHumidity(getAggregationDataNumber("subCategory",
            AssetSubCategory.Humidity.toString(), "subCategory", "asset"));
      data.setSubCategoryIsSmoke(getAggregationDataNumber("subCategory",
            AssetSubCategory.Smoke.toString(), "subCategory", "asset"));
      data.setSubCategoryIsTemperature(getAggregationDataNumber("subCategory",
            AssetSubCategory.Temperature.toString(), "subCategory", "asset"));
      data.setSubCategoryIsWater(getAggregationDataNumber("subCategory",
            AssetSubCategory.Water.toString(), "subCategory", "asset"));
      data.setDashBoardSystemNlyteDetail(getSystemNlyteDetail());
      data.setDashBoardSystemPowerIqDetail(getSystemPowerIqDetail());
      data.setDashBoardSystemSddcServerVcDetail(getSddcServerVcDetail());
      data.setDashBoardSystemSddcServerVroDetail(getSddcServerVroDetail());
      return data;
   }

   public List<DashBoardSystemSddcServerVcDetail> getSddcServerVcDetail() {
      List<DashBoardSystemSddcServerVcDetail> sddcServerDetail = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("VCENTER"));
      List<SDDCSoftwareConfig> sddcSoftwareConfig =
            mongoTemplate.find(query, SDDCSoftwareConfig.class);
      for (SDDCSoftwareConfig s : sddcSoftwareConfig) {
         DashBoardSystemSddcServerVcDetail sddcServerVcDetail =
               new DashBoardSystemSddcServerVcDetail();
         sddcServerVcDetail.setSddcName(s.getName());
         sddcServerVcDetail.setSddcUrl(s.getServerURL());
         sddcServerVcDetail.setNum(getAggregationServerDetail(s.getId(), "vcID"));
         sddcServerDetail.add(sddcServerVcDetail);
      }
      return sddcServerDetail;
   }

   public List<DashBoardSystemSddcServerVroDetail> getSddcServerVroDetail() {
      List<DashBoardSystemSddcServerVroDetail> sddcServerDetail = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("VRO"));
      List<SDDCSoftwareConfig> sddcSoftwareConfig =
            mongoTemplate.find(query, SDDCSoftwareConfig.class);
      for (SDDCSoftwareConfig s : sddcSoftwareConfig) {
         DashBoardSystemSddcServerVroDetail sddcServerVcDetail =
               new DashBoardSystemSddcServerVroDetail();
         sddcServerVcDetail.setSddcName(s.getName());
         sddcServerVcDetail.setSddcUrl(s.getServerURL());
         sddcServerVcDetail.setNum(getAggregationServerDetail(s.getId(), "vroID"));
         sddcServerDetail.add(sddcServerVcDetail);
      }
      return sddcServerDetail;
   }

   public List<DashBoardSystemNlyteDetail> getSystemNlyteDetail() {
      List<DashBoardSystemNlyteDetail> dashBoardSystemDetail = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("Nlyte"));
      List<FacilitySoftwareConfig> facility =
            mongoTemplate.find(query, FacilitySoftwareConfig.class);
      for (FacilitySoftwareConfig f : facility) {
         DashBoardSystemNlyteDetail dashBoardSystem = new DashBoardSystemNlyteDetail();
         dashBoardSystem.setNlyteName(f.getName());
         dashBoardSystem.setNlyteUrl(f.getServerURL());
         dashBoardSystem.setCategoryIsSensorNum(
               getAggregationSystemDetail(f.getId(), AssetCategory.Sensors.toString()));
         dashBoardSystem.setCategoryIsServerNum(
               getAggregationSystemDetail(f.getId(), AssetCategory.Server.toString()));
         dashBoardSystem.setCategoryIsSwitchNum(
               getAggregationSystemDetail(f.getId(), AssetCategory.Networks.toString()));
         dashBoardSystem.setCategoryIsPduNum(
               getAggregationSystemDetail(f.getId(), AssetCategory.PDU.toString()));
         dashBoardSystem.setCategoryIsCabinetNum(
               getAggregationSystemDetail(f.getId(), AssetCategory.Cabinet.toString()));
         dashBoardSystem.setCategoryIsUpsNum(
               getAggregationSystemDetail(f.getId(), AssetCategory.UPS.toString()));
         dashBoardSystemDetail.add(dashBoardSystem);
      }
      return dashBoardSystemDetail;
   }

   public List<DashBoardSystemPowerIqDetail> getSystemPowerIqDetail() {
      List<DashBoardSystemPowerIqDetail> dashBoardSystemPowerIqDetail = new ArrayList<>();
      Query query = new Query();
      query.addCriteria(Criteria.where("type").in("PowerIQ"));
      List<FacilitySoftwareConfig> facility =
            mongoTemplate.find(query, FacilitySoftwareConfig.class);
      for (FacilitySoftwareConfig f : facility) {
         DashBoardSystemPowerIqDetail dashBoardPowerIq = new DashBoardSystemPowerIqDetail();
         dashBoardPowerIq.setPoweriqName(f.getName());
         dashBoardPowerIq.setPoweriqUrl(f.getServerURL());
         dashBoardPowerIq.setPoweriqSensor(
               getAggregationSystemDetail(f.getId(), AssetCategory.Sensors.toString()));
         dashBoardSystemPowerIqDetail.add(dashBoardPowerIq);
      }
      return dashBoardSystemPowerIqDetail;
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
