/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vmware.flowgate.common.model.ServerMapping;

public interface ServerMappingRepository
      extends CouchbasePagingAndSortingRepository<ServerMapping, String> {
   List<ServerMapping> findByAssetNotNull();
   List<ServerMapping> findByAssetIsNull();
   List<ServerMapping> findAllByVroID(String vroID);

   List<ServerMapping> findAllByVcID(String vcID);
   Page<ServerMapping> findAllByVroID(String vroID, Pageable pageable);

   Page<ServerMapping> findAllByVcID(String vcID, Pageable pageable);
}
