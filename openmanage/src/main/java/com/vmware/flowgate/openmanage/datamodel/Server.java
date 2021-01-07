/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Server extends Device{

   @JsonProperty(value="DeviceSpecificData")
   private ServerSpecificData deviceSpecificData;
}
