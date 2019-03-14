/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.vmware.wormhole.common.model.SDDCSoftwareConfig;

public interface SDDCSoftwareRepository extends MongoRepository<SDDCSoftwareConfig, String>,SDDCSoftwareRepositoryOther {

   public Page<SDDCSoftwareConfig> findByUserId(String userId,Pageable pageable);
}
