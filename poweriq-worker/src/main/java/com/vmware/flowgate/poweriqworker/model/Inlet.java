/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Inlet {

   private Long id;
   @JsonProperty(value = "pdu_id")
   private Long pduId;
   private Integer ordinal;
   @JsonProperty(value = "pue_it")
   private boolean pueIt;
   @JsonProperty(value = "pue_total")
   private boolean pueTotal;
   private boolean source;
   @JsonProperty(value = "rated_amps")
   private Double ratedAmps;
   private InletReading reading;
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
   public boolean isSource() {
      return source;
   }
   public void setSource(boolean source) {
      this.source = source;
   }
   public Double getRatedAmps() {
      return ratedAmps;
   }
   public void setRatedAmps(Double ratedAmps) {
      this.ratedAmps = ratedAmps;
   }
   public InletReading getReading() {
      return reading;
   }
   public void setReading(InletReading reading) {
      this.reading = reading;
   }

}
