/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.couchbase.repository.CouchbaseRepository;

import com.vmware.flowgate.common.model.RealTimeData;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRealtimeDataRepository
      extends CouchbaseRepository<RealTimeData, String> {
   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} and assetID = $1 and time between $2 and $2+$3")
   public List<RealTimeData> getDataByIDAndTimeRange(String assetID, long starttime, int duration);
   @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} and time < $1 limit 500 offset 0")
   public List<RealTimeData> getRealTimeDatabtTimeRange(long time);
}
