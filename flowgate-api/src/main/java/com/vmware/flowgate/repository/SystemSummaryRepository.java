/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;

import com.vmware.flowgate.common.model.SystemSummary;

/**
 * @author haoyul
 *
 */
public interface SystemSummaryRepository
      extends CouchbasePagingAndSortingRepository<SystemSummary, String>,
      SystemSummaryRepositoryEnhancement {

}
