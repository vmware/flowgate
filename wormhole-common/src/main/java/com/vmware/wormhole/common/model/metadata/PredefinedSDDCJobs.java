/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.common.model.metadata;

import java.util.Arrays;
import java.util.List;

import com.vmware.wormhole.common.model.JobConfig;
import com.vmware.wormhole.common.model.JobConfig.JobType;

public class PredefinedSDDCJobs {
   public static final List<JobConfig> ALLJobs = Arrays.asList(
//         new JobConfig("VRO-PREDEFINED-METRIC-PROPERTIES-ALERTS-SYNC", "SyncMetricPropsAlert",
//               "SyncJobs", "TenMinutes", "SyncALL", "", "0 0/5 * * * ?",
//               "com.vmware.wormhole.vroworker.scheduler.job.SyncPredefinedMetricPropertyJob",
//               JobType.VRO),
         //         new JobConfig("VC-PREDEFINED-CUSTOM-ATTRIBUTES-SYNC", "SyncHostCustomerAttribute",
         //               "SyncJobs", "ThreeHours", "SyncALL", "", "0 0 0/3 * * ?",
         //               "com.vmware.wormhole.vcworker.scheduler.job.SyncHostMetaDataJob", JobType.VCENTER),
         new JobConfig("AGGREGATOR-PREDEFINED-AGGREGATOR-JOB-DISPATCHER", "AggregateJobDispatcher", "AggregateJobs",
               "Hourly", "AggregatorJob", "", "0 23 * * * ?",
               "com.vmware.wormhole.aggregator.scheduler.job.AggregatorJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-VC-DATA", "SyncVCData", "AggregateJobs",
               "Hourly", "SyncVCAll", "", "0 0/30 * * * ?",
               "com.vmware.wormhole.aggregator.scheduler.job.VCenterJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-VRO-DATA", "SyncVROData", "AggregateJobs",
               "FiveMinutes", "SyncVROAll", "", "0 0/5 * * * ?",
               "com.vmware.wormhole.aggregator.scheduler.job.VROJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-NLYTE-DATA", "SyncNlyteData", "AggregateJobs",
               "FiveMinutes", "SyncNlyteAll", "", "0 0/5 * * * ?",
               "com.vmware.wormhole.aggregator.scheduler.job.NlyteJobDispatcher",
               JobType.AGGREGATOR),
         new JobConfig("AGGREGATOR-PREDEFINED-SYNC-POWERIQ-DATA", "SyncPOWERIQData", "AggregateJobs",
               "FiveMinutes", "SyncPowerIQAll", "", "0 0/5 * * * ?",
               "com.vmware.wormhole.aggregator.scheduler.job.PowerIQJobDispatcher",
               JobType.AGGREGATOR));
//         new JobConfig("NLYTE-PREDEFINED-FETCH-ASSET-TO-WORMHOLE", "SyncAssetData", "AggregateJobs",
//               "Daily", "SyncALL", "", "0 20 23 * * ?",
//               "com.vmware.wormhole.nlyteworker.scheduler.job.SyncAssetDataJob", JobType.NLYTE),
//         new JobConfig("NLYTE-PREDEFINED-UPDATE-MAPPEDASSET-TO-WORMHOLE", "SyncMappedAssetData",
//               "AggregateJobs", "TwoHours", "SyncALL", "", "0 15 0/2 * * ?",
//               "com.vmware.wormhole.nlyteworker.scheduler.job.SyncMappedAssetDataJob",
//               JobType.NLYTE),
//         new JobConfig("NLYTE-PREDEFINED-Sync-REALTIMEDATA", "SyncRealTimeData", "AggregateJobs",
//               "FiveMinutes", "SyncALL", "", "0 0/5 * * * ?",
//               "com.vmware.wormhole.nlyteworker.scheduler.job.SyncRealtimeDataJob", JobType.NLYTE),
//         new JobConfig("POWERIQ-SYNC-REALTIMEDATA", "SyncPowerIQRealTimeData", "AggregateJobs",
//               "FiveMinutes", "SyncALL", "", "0 0/5 * * * ?",
//               "com.vmware.wormhole.poweriqworker.jobs.SyncPowerIQRealTimeDataJob",
//               JobType.POWERIQ),
//         new JobConfig("POWERIQ-SYNC-SENSORDATA", "SyncSensorData", "AggregateJobs", "Daily",
//               "SyncALL", "", "0 35 0 * * ?",
//               "com.vmware.wormhole.poweriqworker.jobs.SyncSensorMetaDataJob", JobType.POWERIQ));
}
