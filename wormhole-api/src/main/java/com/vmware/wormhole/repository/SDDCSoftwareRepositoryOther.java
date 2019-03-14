/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.repository;

import java.util.HashMap;

public interface SDDCSoftwareRepositoryOther {

   int updateSDDCSoftwareByFileds(String id, HashMap<String, Object> fieldsAndValues);
}
