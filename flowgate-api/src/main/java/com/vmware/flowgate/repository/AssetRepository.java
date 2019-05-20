/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.Query;
import org.springframework.data.couchbase.core.query.ViewIndexed;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.vmware.flowgate.common.model.Asset;

@N1qlPrimaryIndexed
@ViewIndexed(designDoc = "asset")
public interface AssetRepository
      extends CouchbasePagingAndSortingRepository<Asset, String> {
   public Asset findOneByAssetNumber(long assetNumber);

   public List<Asset> findByPdusIsNull();

   public Page<Asset> findAssetByCategory(String category, Pageable page);

   @Query("SELECT META(#{#n1ql.bucket}).id AS _ID, META(#{#n1ql.bucket}).cas AS _CAS, #{#n1ql.bucket}.* FROM #{#n1ql.bucket} where _class = 'com.vmware.flowgate.common.model.Asset' and (`assetName` LIKE $1 OR `tag` LIKE $1 ) AND `category` = $2 Limit $3 offset $4")
   public List<Asset> findByAssetNameLikeAndCategoryOrTagLikeAndCategory(String keywords, String category, int pageSize, int offset);

   @Query("SELECT COUNT(_class) AS count FROM #{#n1ql.bucket} WHERE _class = 'com.vmware.flowgate.common.model.Asset' And (`assetName` LIKE $1 OR `tag` LIKE $1) AND `category` = $2 ")
   public long getNumber(String keywords, String category);

   public Asset findOneByAssetName(String name);

   public Page<Asset> findByAssetSourceAndCategory(String assetSource, String category,Pageable page);
}
