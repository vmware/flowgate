/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;

import com.vmware.flowgate.common.model.ServerMapping;

public interface ServerMappingRepository
      extends CouchbasePagingAndSortingRepository<ServerMapping, String> {
   List<ServerMapping> findByAssetNotNull();
   List<ServerMapping> findByAssetIsNull();
}
