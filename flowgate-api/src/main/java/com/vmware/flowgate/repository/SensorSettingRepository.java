/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;

import com.vmware.flowgate.common.model.SensorSetting;

public interface SensorSettingRepository extends
      CouchbasePagingAndSortingRepository<SensorSetting, String> {
   List<SensorSetting> findAllByType(String type);
}
