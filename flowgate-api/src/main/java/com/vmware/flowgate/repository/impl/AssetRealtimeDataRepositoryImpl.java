/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.repository.AssetRealtimeDataExpert;

public class AssetRealtimeDataRepositoryImpl implements AssetRealtimeDataExpert {

   @Autowired
   MongoTemplate mongoTemplate;

   @Override
   public List<RealTimeData> getDataByIDAndTimeRange(String assetID, long starttime, int duration) {
      Criteria criteria = new Criteria();
      Query query = new Query(criteria.andOperator(Criteria.where("assetID").is(assetID),
            Criteria.where("time").gte(starttime),
            Criteria.where("time").lte(starttime + duration)));
      return mongoTemplate.find(query, RealTimeData.class);
   }

}
