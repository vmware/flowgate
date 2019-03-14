/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.vmware.wormhole.common.model.ServerMapping;

public interface ServerMappingRepository extends MongoRepository<ServerMapping, String> {
   List<ServerMapping> findByAssetNotNull();
   List<ServerMapping> findByAssetIsNull();
}
