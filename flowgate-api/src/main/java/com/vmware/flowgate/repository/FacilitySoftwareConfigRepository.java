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

import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import org.springframework.stereotype.Repository;

@Repository
public interface FacilitySoftwareConfigRepository extends
            CouchbaseRepository<FacilitySoftwareConfig, String> {
   FacilitySoftwareConfig findOneByServerURL(String serverURL);

   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND `type` = $1")
   List<FacilitySoftwareConfig> findAllByType(String type);

   Page<FacilitySoftwareConfig> findALlByUserId(String userId, Pageable page);

   Page<FacilitySoftwareConfig> findAllByUserIdAndTypeIn(String userId, List<String> types, Pageable page);

   Page<FacilitySoftwareConfig> findAllByTypeIn(List<String> types, Pageable page);

   @Query("SELECT COUNT(*) AS count FROM #{#n1ql.bucket} WHERE _class = 'com.vmware.flowgate.common.model.FacilitySoftwareConfig' and subCategory = $1")
   int countFacilityBySubcategory(String subcategory);
}
