/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.ViewIndexed;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.model.Asset;

@N1qlPrimaryIndexed
@ViewIndexed(designDoc = "asset")
public interface AssetRepository
      extends CouchbasePagingAndSortingRepository<Asset, String>, AssetRepositoryEnhancement {
   public Asset findOneByAssetNumber(long assetNumber);

   public List<Asset> findByPdusIsNull();

   public List<Asset> findByCategory(AssetCategory category);

   public Page<Asset> findByAssetNameLikeAndCategoryOrTagLikeAndCategory(String assetName,
         AssetCategory category1, String tag, AssetCategory category, Pageable pageable);

   public Asset findOneByAssetName(String name);
}
