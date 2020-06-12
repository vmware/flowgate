/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlowgateApiApplication {

   public static void main(String[] args) {
      SpringApplication.run(FlowgateApiApplication.class, args);
      System.out.println(">>>>>>>>>>>>>>>>Welcome to use API service!<<<<<<<<<<<<<<<<");
   }
}