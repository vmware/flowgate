/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.vmware.wormhole.common.AssetCategory;
import com.vmware.wormhole.common.model.Asset;

public interface AssetRepository
      extends MongoRepository<Asset, String>, AssetRepositoryEnhancement {
   public Asset findOneByAssetNumber(long assetNumber);

   public List<Asset> findByPdusIsNull();

   public List<Asset> findByCategory(AssetCategory category);

   public Page<Asset> findByAssetNameLikeAndCategoryOrTagLikeAndCategory(String assetName,
         AssetCategory category1, String tag, AssetCategory category, Pageable pageable);

   public Asset findOneByAssetName(String name);
}
