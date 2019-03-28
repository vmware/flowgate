/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.vmware.flowgate.common.model.ServerMapping;

public interface ServerMappingRepository extends MongoRepository<ServerMapping, String> {
   List<ServerMapping> findByAssetNotNull();
   List<ServerMapping> findByAssetIsNull();
}
