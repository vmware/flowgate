/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.common.model.redis.message;

import java.io.Serializable;

public interface EventSource extends Serializable {

   EventUser getUser();
}
