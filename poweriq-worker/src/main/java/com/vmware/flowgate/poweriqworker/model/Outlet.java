/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Outlet {
   private Long id;
   private Long ordinal;
   private String name;
   @JsonProperty(value = "device_id")
   private Long deviceId;
   @JsonProperty(value = "pdu_id")
   private Long pduId;
   private String state;
   @JsonProperty(value = "rated_amps")
   private Double ratedAmps;
   private OutletReading reading;
   public Long getId() {
      return id;
   }
   public void setId(Long id) {
      this.id = id;
   }
   public Long getOrdinal() {
      return ordinal;
   }
   public void setOrdinal(Long ordinal) {
      this.ordinal = ordinal;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public Long getDeviceId() {
      return deviceId;
   }
   public void setDeviceId(Long deviceId) {
      this.deviceId = deviceId;
   }
   public Long getPduId() {
      return pduId;
   }
   public void setPduId(Long pduId) {
      this.pduId = pduId;
   }
   public String getState() {
      return state;
   }
   public void setState(String state) {
      this.state = state;
   }
   public Double getRatedAmps() {
      return ratedAmps;
   }
   public void setRatedAmps(Double ratedAmps) {
      this.ratedAmps = ratedAmps;
   }
   public OutletReading getReading() {
      return reading;
   }
   public void setReading(OutletReading reading) {
      this.reading = reading;
   }

}
