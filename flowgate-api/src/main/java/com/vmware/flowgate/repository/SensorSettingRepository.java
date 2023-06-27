/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.List;

import org.springframework.data.couchbase.repository.CouchbaseRepository;

import com.vmware.flowgate.common.model.SensorSetting;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorSettingRepository extends
            CouchbaseRepository<SensorSetting, String> {
   List<SensorSetting> findAllByType(String type);
}
