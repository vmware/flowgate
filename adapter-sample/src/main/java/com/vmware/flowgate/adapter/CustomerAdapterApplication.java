/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages="com.vmware.flowgate")
public class CustomerAdapterApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerAdapterApplication.class, args);
	}
}