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

import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import org.springframework.stereotype.Repository;

@Repository
public interface SDDCSoftwareRepository extends
            CouchbaseRepository<SDDCSoftwareConfig, String> {

   Page<SDDCSoftwareConfig> findAllByUserId(String userId, Pageable pageable);

   List<SDDCSoftwareConfig> findAllByUserId(String userId);

   SDDCSoftwareConfig findOneByServerURL(String serverURL);

   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND `type` = $1")
   List<SDDCSoftwareConfig> findAllByType(String type);

   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND `userId` = $1 AND `type` = $2")
   List<SDDCSoftwareConfig> findAllByUserIdAndType(String userId, String type);

}
