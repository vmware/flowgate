/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.HashMap;
import java.util.List;

import com.vmware.flowgate.common.model.Asset;

public interface AssetRepositoryEnhancement {
   int updateAssetByFileds(String id, HashMap<String, Object> fieldsAndValues);
   List<Asset> findByIDs(List<String>IDs);
}
