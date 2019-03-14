/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WormholeApiApplication {

   public static void main(String[] args) {
      SpringApplication.run(WormholeApiApplication.class, args);
      System.out.println(">>>>>>>>>>>>>>>>Welcome to use API service!<<<<<<<<<<<<<<<<");
   }
}
