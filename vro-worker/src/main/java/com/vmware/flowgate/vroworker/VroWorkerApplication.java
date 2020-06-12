/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages={"com.vmware.flowgate"})
public class VroWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(VroWorkerApplication.class, args);
		System.out.println(">>>>>>>>>>>>>>>>Welcome to use VROPS worker!<<<<<<<<<<<<<<<<");
	}
}