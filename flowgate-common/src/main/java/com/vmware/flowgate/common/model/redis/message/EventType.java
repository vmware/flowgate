/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model.redis.message;

public enum EventType {
   InfoBlox("InfoBloxAction"),
   PowerIQ("PowerIQAction"),
   VCenter("VCenterAction"),
   VROps("VROpsAction"),
   Nlyte("NlyteAction"),
   Aggregator("AggregatorAction"),
   Labsdb("LabsdbAction"),
   OpenManage("OpenManageAction");

   private final String desc;

   EventType(String desc) {
      this.desc = desc;
   }
}
