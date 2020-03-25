/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.flowgate.common.model.SystemSummary;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.service.SummaryService;

@RestController
@RequestMapping("/v1/summary")
public class SystemSummaryController {

   @Autowired
   SummaryService summaryService;

   @RequestMapping(value = "/systemsummary", method = RequestMethod.GET)
   public SystemSummary getAllDashBoardData(
         @RequestParam(value = "usecache", required = false) Boolean usecache) {
      SystemSummary result = null;
      if(usecache == null) {
         usecache = true;
      }
      try {
         result = summaryService.getSystemResult(usecache);
      } catch (IOException e) {
         throw new WormholeRequestException("Failed to get summary data.");
      }
      return result;
   }
}
