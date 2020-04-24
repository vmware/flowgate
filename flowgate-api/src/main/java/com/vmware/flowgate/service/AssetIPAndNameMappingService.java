/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ScanOptions.ScanOptionsBuilder;
import org.springframework.stereotype.Service;

import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.repository.AssetIPMappingRepository;
import com.vmware.flowgate.util.BaseDocumentUtil;

@Service
public class AssetIPAndNameMappingService {

   private static final Logger logger = LoggerFactory.getLogger(AssetIPAndNameMappingService.class);

   public static final String SERVER_ASSET_NAME_LIST = "asset:servernamelist";
   private static final int SERVER_ASSET_NAME_TIME_OUT = 7200;
   private static final int LIMIT_RESULT = 100;

   @Autowired
   private AssetIPMappingRepository assetIPMappingRepository;

   @Autowired
   private RedisService redisService;

   @Autowired
   private AssetService assetService;

   public boolean create(AssetIPMapping mapping) {
      BaseDocumentUtil.generateID(mapping);
      if(redisService.hasKey(SERVER_ASSET_NAME_LIST)) {
         String assetName =  mapping.getAssetname();
         if(redisService.isMember(SERVER_ASSET_NAME_LIST, assetName)) {
            assetIPMappingRepository.save(mapping);
         }else {
            logger.error("Not exist asset with name : " + assetName);
            return false;
         }
      }else {
         Set<String> assetNames = getAllAssetNames(AssetCategory.Server);
         if(assetNames.contains(mapping.getAssetname())) {
            assetIPMappingRepository.save(mapping);
         }else {
            return false;
         }
         redisService.opsForSetToAdd(SERVER_ASSET_NAME_LIST, SERVER_ASSET_NAME_TIME_OUT, assetNames.toArray(new String[assetNames.size()]));
      }
      return true;
   }

   public List<String> searchAssetName(String content){
      if(redisService.hasKey(SERVER_ASSET_NAME_LIST)) {
         ScanOptionsBuilder builder = ScanOptions.scanOptions();
         builder.count(redisService.getValueSize(SERVER_ASSET_NAME_LIST));
         builder.match("*"+content+"*");
         List<String> allMatchResult = redisService.scan(SERVER_ASSET_NAME_LIST, builder.build());
         if(allMatchResult.size() > LIMIT_RESULT) {
            return allMatchResult.subList(0, LIMIT_RESULT + 1);
         }
         return allMatchResult;
      }
      Set<String> assetNames = getAllAssetNames(AssetCategory.Server);
      redisService.opsForSetToAdd(SERVER_ASSET_NAME_LIST, SERVER_ASSET_NAME_TIME_OUT, assetNames.toArray(new String[assetNames.size()]));
      if(assetNames.size() > LIMIT_RESULT) {
         return new ArrayList<String>(assetNames).subList(0, LIMIT_RESULT + 1);
      }
      return new ArrayList<String>(assetNames);
   }

   private Set<String> getAllAssetNames(AssetCategory category){
      Set<String> assetNames = new HashSet<String>();
      int pageSize = 200;
      int pageNumber = 1;
      Page<Asset> assetPages = assetService.getAssetByCategory(category, pageSize, pageNumber);
      while (assetPages.hasNext()) {
         for(Asset asset : assetPages.getContent()) {
            assetNames.add(asset.getAssetName());
         }
         pageNumber++;
         assetPages = assetService.getAssetByCategory(category, pageSize, pageNumber);
      }
      return assetNames;
   }

}
