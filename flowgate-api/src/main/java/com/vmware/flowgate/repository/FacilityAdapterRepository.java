/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.Query;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;

import com.vmware.flowgate.common.model.FacilityAdapter;

@N1qlPrimaryIndexed
public interface FacilityAdapterRepository extends CouchbasePagingAndSortingRepository<FacilityAdapter, String> {
   FacilityAdapter findByDisplayName(String displayName);
   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter}")
   List<FacilityAdapter> findAllFacilityAdapters();
}
