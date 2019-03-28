/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.labsdb.common;
import java.io.Serializable;

import com.vmware.flowgate.common.FlowgateConstant;

public class EndDevice implements Serializable{
   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private String startPort;
   private WireMapType wireMapType;
   private String endDeviceName;
   private String endDeviceAssetId;
   private String endPort;

   public enum WireMapType {
      fiber, ilo, iscsi, net, power, serial
   }

   public String getStartPort() {
      return startPort;
   }

   public void setStartPort(String startPort) {
      this.startPort = startPort;
   }

   public WireMapType getWireMapType() {
      return wireMapType;
   }

   public void setWireMapType(WireMapType wireMapType) {
      this.wireMapType = wireMapType;
   }

   public String getEndDeviceName() {
      return endDeviceName;
   }

   public void setEndDeviceName(String endDeviceName) {
      this.endDeviceName = endDeviceName;
   }

   public String getEndDeviceAssetId() {
      return endDeviceAssetId;
   }

   public void setEndDeviceAssetId(String endDeviceAssetId) {
      this.endDeviceAssetId = endDeviceAssetId;
   }

   public String getEndPort() {
      return endPort;
   }

   public void setEndPort(String endPort) {
      this.endPort = endPort;
   }
   
   @Override
   public String toString() {
      return this.startPort+FlowgateConstant.SEPARATOR+this.endDeviceName+FlowgateConstant.SEPARATOR
            +this.endPort+FlowgateConstant.SEPARATOR+this.endDeviceAssetId;
   }

}
