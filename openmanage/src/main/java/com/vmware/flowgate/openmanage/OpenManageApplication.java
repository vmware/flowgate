/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages="com.vmware.flowgate")
public class OpenManageApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenManageApplication.class, args);
	}
}