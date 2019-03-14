/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.common.model;

public class ServerSensorData {
   private ServerSensorType type;
   private double valueNum;
   private String value;
   private long timeStamp;

   public ServerSensorType getType() {
      return type;
   }

   public void setType(ServerSensorType type) {
      this.type = type;
   }

   public double getValueNum() {
      return valueNum;
   }

   public void setValueNum(double valueNum) {
      this.valueNum = valueNum;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public enum ServerSensorType {
      FRONTPANELTEMP, BACKPANELTEMP, HUMIDITY, PDU_RealtimeVoltage, PDU_RealtimePower, PDU_RealtimeLoad,PDU_RealtimeVoltagePercent,PDU_RealtimePowerPercent,PDU_RealtimeLoadPercent
   }

   public long getTimeStamp() {
      return timeStamp;
   }

   public void setTimeStamp(long timeStamp) {
      this.timeStamp = timeStamp;
   }
}
