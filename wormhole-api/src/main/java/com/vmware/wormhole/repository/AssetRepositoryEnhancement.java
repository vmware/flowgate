/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.repository;

import java.util.HashMap;
import java.util.List;

import com.vmware.wormhole.common.model.Asset;
import com.vmware.wormhole.common.model.DashBoardData;

public interface AssetRepositoryEnhancement {
   int updateAssetByFileds(String id, HashMap<String, Object> fieldsAndValues);

   List<Asset> findByIDs(List<String> IDs);

   DashBoardData getAllDashBoardData();
}
