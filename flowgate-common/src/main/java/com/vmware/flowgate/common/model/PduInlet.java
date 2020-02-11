/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class PduInlet {

   private Long id;
   private Long pduId;
   private Integer ordinal;
   private String formatedName;
   private boolean powerSource;
   private Double ratedAmps;
   private boolean pueIt;
   private boolean pueTotal;

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
   public boolean isPowerSource() {
      return powerSource;
   }
   public void setPowerSource(boolean powerSource) {
      this.powerSource = powerSource;
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
   public boolean isPueIt() {
      return pueIt;
   }
   public void setPueIt(boolean pueIt) {
      this.pueIt = pueIt;
   }
   public boolean isPueTotal() {
      return pueTotal;
   }
   public void setPueTotal(boolean pueTotal) {
      this.pueTotal = pueTotal;
   }

}
