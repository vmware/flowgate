/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.vmware.flowgate.common.model.metadata.PredefinedSDDCJobs;
import com.vmware.flowgate.repository.JobsRepository;

@Component
public class PredefinedDataLoader implements CommandLineRunner {

   @Autowired
   private JobsRepository jobsRepository;

   @Override
   public void run(String... args) throws Exception {
      syncPredefinedJobs();
   }

   private void syncPredefinedJobs() {
      jobsRepository.saveAll(PredefinedSDDCJobs.ALLJobs);
   }


}
