/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model.redis.message;

public interface AsyncService {
   void executeAsync(EventMessage message);
}
