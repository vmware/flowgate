/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.vmware.flowgate.common.model.JobConfig;

public interface JobsRepository extends MongoRepository<JobConfig, String> {

}
