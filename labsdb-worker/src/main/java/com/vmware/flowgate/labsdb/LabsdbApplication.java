/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.labsdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages="com.vmware.flowgate")
public class LabsdbApplication {

	public static void main(String[] args) {
		SpringApplication.run(LabsdbApplication.class, args);
	}
}