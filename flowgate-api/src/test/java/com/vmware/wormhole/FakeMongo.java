/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;

@Configuration
public class FakeMongo extends AbstractMongoConfiguration {

   @Override
   protected String getDatabaseName() {
      return "mockDB";
   }

   @Override
   @Bean
   public MongoClient mongo() {
      Fongo fongo = new Fongo("mockDB");
      return fongo.getMongo();
   }

}

