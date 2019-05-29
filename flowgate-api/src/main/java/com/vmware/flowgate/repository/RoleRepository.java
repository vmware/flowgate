/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;


import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;

import com.vmware.flowgate.common.model.WormholeRole;

@N1qlPrimaryIndexed
public interface RoleRepository extends CouchbasePagingAndSortingRepository<WormholeRole, String> {
   WormholeRole findOneByRoleName(String roleName);
}
