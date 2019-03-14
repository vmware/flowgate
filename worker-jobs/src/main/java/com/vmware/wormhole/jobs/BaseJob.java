/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.jobs;

import org.springframework.web.context.support.SpringBeanAutowiringSupport;

public class BaseJob {

   protected void springBeanAutowiringSupport() {
      SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
   }

}
