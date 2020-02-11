/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class PduInlet {

   private Long id;
   private Long pduId;
   private Integer ordinal;
   private String formatedName;
   private boolean pduPower;
   private Double ratedAmps;

   public Long getId() {
      return id;
   }
   public void setId(Long id) {
      this.id = id;
   }
   public Long getPduId() {
      return pduId;
   }
   public void setPduId(Long pduId) {
      this.pduId = pduId;
   }
   public Integer getOrdinal() {
      return ordinal;
   }
   public void setOrdinal(Integer ordinal) {
      this.ordinal = ordinal;
   }
   public boolean isPduPower() {
      return pduPower;
   }
   public void setPduPower(boolean pduPower) {
      this.pduPower = pduPower;
   }
   public Double getRatedAmps() {
      return ratedAmps;
   }
   public void setRatedAmps(Double ratedAmps) {
      this.ratedAmps = ratedAmps;
   }
   public String getFormatedName() {
      return formatedName;
   }
   public void setFormatedName(String formatedName) {
      this.formatedName = formatedName;
   }

}
