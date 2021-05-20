/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model.metadata;

import java.util.Arrays;
import java.util.List;

import com.vmware.flowgate.common.model.JobConfig;
import com.vmware.flowgate.common.model.JobConfig.JobType;

public class PredefinedSDDCJobs {
   public static final List<JobConfig> ALLJobs = Arrays.asList(
         new JobConfig("AGGREGATOR-PREDEFINED-AGGREGATOR-JOB-DISPATCHER", "AggregateJobDispatcher", "AggregateJobs",
               "Hourly", "AggregatorJob", "", "0 23 * * * ?",
               "com.vmware.flowgate.aggregator.scheduler.job.AggregatorJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-VC-DATA", "SyncVCData", "AggregateJobs",
               "Hourly", "SyncVCAll", "", "10 0/5 * * * ? *",
               "com.vmware.flowgate.aggregator.scheduler.job.VCenterJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-VRO-DATA", "SyncVROData", "AggregateJobs",
               "FiveMinutes", "SyncVROAll", "", "59 0/5 * * * ?",
               "com.vmware.flowgate.aggregator.scheduler.job.VROJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-NLYTE-DATA", "SyncNlyteData", "AggregateJobs",
               "FiveMinutes", "SyncNlyteAll", "", "1 0/5 * * * ?",
               "com.vmware.flowgate.aggregator.scheduler.job.NlyteJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-POWERIQ-DATA", "SyncPOWERIQData", "AggregateJobs",
               "FiveMinutes", "SyncPowerIQAll", "", "1 0/5 * * * ?",
               "com.vmware.flowgate.aggregator.scheduler.job.PowerIQJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-LABSDB-DATA", "SyncLABSDBData", "AggregateJobs",
               "Everyday", "SyncLABSDBAll", "", "0 18 22 * * ?",
               "com.vmware.flowgate.aggregator.scheduler.job.LabsdbJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-CUSTOMER-ADAPTER-DATA", "SyncCustomerAdapterData", "AggregateJobs",
               "FiveMinutes", "SyncAdapterData", "", "10 0/5 * * * ?",
               "com.vmware.flowgate.aggregator.scheduler.job.CustomerAdapterJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-OPENMANAGE-DATA", "SyncOpenmanageData", "AggregateJobs",
               "FiveMinutes", "SyncOpenmanageMetricData", "", "1 0/5 * * * ?",
               "com.vmware.flowgate.aggregator.scheduler.job.OpenmanageJobDispatcher",
               JobType.AGGREGATOR));
}
