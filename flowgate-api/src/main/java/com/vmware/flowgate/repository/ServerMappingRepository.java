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

import com.vmware.flowgate.common.model.ServerMapping;

@N1qlPrimaryIndexed
@ViewIndexed(designDoc = "serverMapping")
public interface ServerMappingRepository
      extends CouchbasePagingAndSortingRepository<ServerMapping, String> {
   List<ServerMapping> findByAssetNotNull();
   
   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND `asset` IS MISSING OR `asset` IS NULL")
   List<ServerMapping> findByAssetIsNull();
   
   List<ServerMapping> findAllByVroID(String vroID);

   List<ServerMapping> findAllByVcID(String vcID);
   
   Page<ServerMapping> findAllByVroID(String vroID, Pageable pageable);

   Page<ServerMapping> findAllByVcID(String vcID, Pageable pageable);
}
