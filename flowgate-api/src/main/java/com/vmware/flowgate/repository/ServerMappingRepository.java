/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vmware.flowgate.common.model.ServerMapping;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerMappingRepository
      extends CouchbaseRepository<ServerMapping, String> {
   List<ServerMapping> findByAssetNotNull();

   Page<ServerMapping> findAllByVroID(String vroID, Pageable pageable);

   Page<ServerMapping> findAllByVcID(String vcID, Pageable pageable);

   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND (`asset` IS MISSING OR `asset` IS NULL)")
   List<ServerMapping> findByAssetIsNull();

   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND `vroID` = $1")
   List<ServerMapping> findAllByVroID(String vroID);

   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND `vcID` = $1")
   List<ServerMapping> findAllByVCID(String vcID);
}
