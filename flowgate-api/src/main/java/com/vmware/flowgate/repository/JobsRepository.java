/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.couchbase.repository.CouchbaseRepository;

import com.vmware.flowgate.common.model.JobConfig;
import org.springframework.stereotype.Repository;

@Repository
public interface JobsRepository extends CouchbaseRepository<JobConfig, String> {
   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND `jobType` = $1")
   Iterable<JobConfig> findAllByJobType(String jobType);
}
