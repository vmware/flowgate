/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.vcworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages= {"com.vmware.wormhole"})
public class VcWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(VcWorkerApplication.class, args);
	}
}