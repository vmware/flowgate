/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;

import com.vmware.flowgate.common.model.PrivilegeResourceMapping;

@N1qlPrimaryIndexed
public interface PrivilegeResourcesMappingReposity
      extends CouchbasePagingAndSortingRepository<PrivilegeResourceMapping, String> {
}
