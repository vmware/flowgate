/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.ViewIndexed;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig.SoftwareType;

@N1qlPrimaryIndexed
@ViewIndexed(designDoc = "sDDCSoftwareConfig")
public interface SDDCSoftwareRepository extends
      CouchbasePagingAndSortingRepository<SDDCSoftwareConfig, String> {

   Page<SDDCSoftwareConfig> findAllByUserId(String userId, Pageable pageable);

   List<SDDCSoftwareConfig> findAllByUserId(String userId);
   SDDCSoftwareConfig findOneByServerURL(String serverURL);

   List<SDDCSoftwareConfig> findAllByType(SoftwareType type);

   List<SDDCSoftwareConfig> findAllByUserIdAndType(String userId, SoftwareType type);

   SDDCSoftwareConfig findOneByIdAndUserId(String id, String userId);
}
