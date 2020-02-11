/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class PduOutlet {

   private Long id;
   private Long ordinal;
   private String name;
   private String formatedName;//outlet_prefix + ordinal, eg:OUTLET1
   private Long deviceId;
   private Long pduId;
   private String state;
   private Double ratedAmps;
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
   public String getFormatedName() {
      return formatedName;
   }
   public void setFormatedName(String formatedName) {
      this.formatedName = formatedName;
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

}
