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

   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND `category` = $1")
   public List<Asset> findAssetsByCategory(String category);

   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND (`assetName` LIKE $1 OR `tag` LIKE $1) AND `category` = $2")
   public Page<Asset> findByAssetNameLikeAndCategoryOrTagLikeAndCategory(String keywords, String category, Pageable pageable);

   public Asset findOneByAssetName(String name);

   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND (`assetSource` = $1 AND `category` = $2)")
   public List<Asset> findAllByAssetSourceAndCategory(String assetSource, String category);
}
