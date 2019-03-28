/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Reading {

   @JsonProperty(value = "inlet_readings")
   private List<InletReading> inletReadings;
   @JsonProperty(value = "inlet_pole_readings")
   private List<InletPoleReading> inletPoleReadings;
   @JsonProperty(value = "circuit_breaker_readings")
   private List<CircuitBreakerReading> circuitBreakerReadings;

   public List<InletReading> getInletReadings() {
      return inletReadings;
   }

   public void setInletReadings(List<InletReading> inletReadings) {
      this.inletReadings = inletReadings;
   }

   public List<InletPoleReading> getInletPoleReadings() {
      return inletPoleReadings;
   }

   public void setInletPoleReadings(List<InletPoleReading> inletPoleReadings) {
      this.inletPoleReadings = inletPoleReadings;
   }

   public List<CircuitBreakerReading> getCircuitBreakerReadings() {
      return circuitBreakerReadings;
   }

   public void setCircuitBreakerReadings(List<CircuitBreakerReading> circuitBreakerReadings) {
      this.circuitBreakerReadings = circuitBreakerReadings;
   }

}
