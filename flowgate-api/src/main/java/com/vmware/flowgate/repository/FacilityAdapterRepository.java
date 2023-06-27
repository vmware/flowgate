/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.couchbase.repository.CouchbaseRepository;

import com.vmware.flowgate.common.model.FacilityAdapter;
import org.springframework.stereotype.Repository;

@Repository
public interface FacilityAdapterRepository extends
            CouchbaseRepository<FacilityAdapter, String> {
   FacilityAdapter findByDisplayName(String displayName);
   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter}")
   List<FacilityAdapter> findAllFacilityAdapters();
}
