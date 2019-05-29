/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;

import com.vmware.flowgate.common.model.AssetIPMapping;

@N1qlPrimaryIndexed
public interface AssetIPMappingRepository extends CouchbasePagingAndSortingRepository<AssetIPMapping, String> {
   List<AssetIPMapping> findAllByIp(String id);
}
