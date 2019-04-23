/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.ViewIndexed;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.vmware.flowgate.common.model.FacilitySoftwareConfig;

@N1qlPrimaryIndexed
@ViewIndexed(designDoc = "facilitySoftwareConfig")
public interface FacilitySoftwareConfigRepository extends CouchbasePagingAndSortingRepository<FacilitySoftwareConfig, String>{

}
