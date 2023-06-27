/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.couchbase.client.java.json.JsonArray;
import com.vmware.flowgate.common.model.Asset;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository
      extends CouchbaseRepository<Asset, String> {
   public Asset findOneByAssetNumber(long assetNumber);

   public List<Asset> findByPdusIsNull();

   public Page<Asset> findAssetByCategory(String category, Pageable page);

   @Query("SELECT META(#{#n1ql.bucket}).id AS _ID, META(#{#n1ql.bucket}).cas AS _CAS, #{#n1ql.bucket}.* FROM #{#n1ql.bucket} where _class = 'com.vmware.flowgate.common.model.Asset' AND `category` = $2 and `assetName` LIKE $1  Limit $3 offset $4")
   public List<Asset> findByAssetNameLikeAndCategory(String keywords, String category, int pageSize, int offset);

   @Query("SELECT COUNT(*) AS count FROM #{#n1ql.bucket} WHERE _class = 'com.vmware.flowgate.common.model.Asset' AND `category` = $2  And `assetName` LIKE $1")
   public long getNumber(String keywords, String category);

   public Page<Asset> findByAssetSource(String assetSource, Pageable page);

   @Query("SELECT META(#{#n1ql.bucket}).id AS _ID, META(#{#n1ql.bucket}).cas AS _CAS, #{#n1ql.bucket}.* FROM #{#n1ql.bucket} use keys $1")
   public List<Asset> findAll(JsonArray assetIds);

   public Asset findOneByAssetName(String name);

   public Asset findOneByAssetNumberAndAssetName(long assetNumber,String name);

   public Page<Asset> findByAssetSourceContainingAndCategory(String assetSource, String category,Pageable page);

   @Query("SELECT META(#{#n1ql.bucket}).id AS _ID, META(#{#n1ql.bucket}).cas AS _CAS, #{#n1ql.bucket}.assetName FROM #{#n1ql.bucket} where _class = 'com.vmware.flowgate.common.model.Asset' AND `category` = $1")
   public List<Asset> findAssetNameByCategory(String category);
}
