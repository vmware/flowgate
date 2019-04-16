/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.flowgate.common.model.SystemSummary;
import com.vmware.flowgate.repository.SystemSummaryRepository;

@RestController
@RequestMapping("/v1/summary")
public class SystemSummaryController {

   @Autowired
   SystemSummaryRepository systemSummaryRepository;

   @RequestMapping(value = "/systemsummary", method = RequestMethod.GET)
   public SystemSummary getAllDashBoardData() {
      return systemSummaryRepository.getSystemResult();
   }
}
