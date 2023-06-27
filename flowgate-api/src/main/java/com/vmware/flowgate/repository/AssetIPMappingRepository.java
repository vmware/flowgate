/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vmware.flowgate.common.model.AssetIPMapping;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetIPMappingRepository extends
            CouchbaseRepository<AssetIPMapping, String> {
   List<AssetIPMapping> findAllByIp(String id);
   Page<AssetIPMapping> findByIp(String ip, Pageable page);
}
