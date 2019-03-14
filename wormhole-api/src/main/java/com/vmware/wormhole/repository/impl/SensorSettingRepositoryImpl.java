/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.repository.impl;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.WriteResult;
import com.vmware.wormhole.common.model.SensorSetting;
import com.vmware.wormhole.repository.SensorSettingRepositoryOther;

public class SensorSettingRepositoryImpl implements SensorSettingRepositoryOther{

   @Autowired
   MongoTemplate mongoTemplate;
   @Override
   public int updateSensorSettingByFileds(String id, HashMap<String, Object> fieldsAndValues) {
      // TODO Auto-generated method stub
      Query query = new Query(Criteria.where("id").is(id));
      Update update = new Update();
      for (String key : fieldsAndValues.keySet()) {
         update.set(key, fieldsAndValues.get(key));
      }

      WriteResult result = mongoTemplate.updateFirst(query, update, SensorSetting.class);

      if (result != null) {
         return result.getN();
      }
     return 0;
   }
}
