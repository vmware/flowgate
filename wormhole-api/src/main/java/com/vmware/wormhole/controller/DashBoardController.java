/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.wormhole.common.model.DashBoardData;
import com.vmware.wormhole.repository.AssetRepository;

@RestController
@RequestMapping("/v1/dashboard")
public class DashBoardController {

   @Autowired
   AssetRepository assetRepository;

   @RequestMapping(value = "/alldashboarddata", method = RequestMethod.GET)
   public DashBoardData getAllDashBoardData() {
      return assetRepository.getAllDashBoardData();
   }
}
