/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.Query;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;

import com.vmware.flowgate.common.model.RealTimeData;

@N1qlPrimaryIndexed
public interface AssetRealtimeDataRepository
      extends CouchbasePagingAndSortingRepository<RealTimeData, String> {
   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} and assetID = $1 and time between $2 and $2+$3")
   public List<RealTimeData> getDataByIDAndTimeRange(String assetID, long starttime, long duration);
}
