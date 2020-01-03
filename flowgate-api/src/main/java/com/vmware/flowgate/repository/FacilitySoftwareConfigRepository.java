/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.Query;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vmware.flowgate.common.model.FacilitySoftwareConfig;

@N1qlPrimaryIndexed
public interface FacilitySoftwareConfigRepository extends CouchbasePagingAndSortingRepository<FacilitySoftwareConfig, String>{
   FacilitySoftwareConfig findOneByServerURL(String serverURL);

   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND `type` = $1")
   List<FacilitySoftwareConfig> findAllByType(String type);

   Page<FacilitySoftwareConfig> findALlByUserId(String userId, Pageable page);

   Page<FacilitySoftwareConfig> findAllByUserIdAndTypeIn(String userId, List<String> types, Pageable page);

   Page<FacilitySoftwareConfig> findAllByTypeIn(List<String> types, Pageable page);
}
