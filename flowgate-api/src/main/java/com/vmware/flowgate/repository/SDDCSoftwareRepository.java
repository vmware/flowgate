/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.ViewIndexed;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vmware.flowgate.common.model.SDDCSoftwareConfig;

@N1qlPrimaryIndexed
@ViewIndexed(designDoc = "sDDCSoftwareConfig")
public interface SDDCSoftwareRepository extends
      CouchbasePagingAndSortingRepository<SDDCSoftwareConfig, String>, SDDCSoftwareRepositoryOther {

   public Page<SDDCSoftwareConfig> findByUserId(String userId,Pageable pageable);
}
