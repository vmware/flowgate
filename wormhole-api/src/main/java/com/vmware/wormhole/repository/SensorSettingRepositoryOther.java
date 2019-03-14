/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.repository;

import java.util.HashMap;

public interface SensorSettingRepositoryOther {
   int updateSensorSettingByFileds(String id, HashMap<String, Object> fieldsAndValues);
}
