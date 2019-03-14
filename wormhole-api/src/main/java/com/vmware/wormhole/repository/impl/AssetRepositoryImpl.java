/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.repository.impl;

import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.WriteResult;
import com.vmware.wormhole.common.model.Asset;
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

}
