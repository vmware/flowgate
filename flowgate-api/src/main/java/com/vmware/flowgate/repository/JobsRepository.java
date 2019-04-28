/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.Query;
import org.springframework.data.couchbase.core.query.ViewIndexed;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;
import com.vmware.flowgate.common.model.JobConfig;
import com.vmware.flowgate.common.model.JobConfig.JobType;

@N1qlPrimaryIndexed
@ViewIndexed(designDoc = "jobConfig")
public interface JobsRepository extends CouchbasePagingAndSortingRepository<JobConfig, String> {
   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} AND `jobType` = $1")
   Iterable<JobConfig> findAllByJobType(String jobType);
}
