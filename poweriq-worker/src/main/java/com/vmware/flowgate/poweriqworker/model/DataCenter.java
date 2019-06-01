/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataCenter {

   private long id;
   private String name;
   @JsonProperty(value = "company_name")
   private String companyName;
   @JsonProperty(value = "contact_name")
   private String contactName;
   @JsonProperty(value = "contact_phone")
   private String contactPhone;
   @JsonProperty(value = "contact_email")
   private String contactEmail;
   private String city;
   private String state;
   private String country;
   @JsonProperty(value = "peak_kwh_rate")
   private Double peakKwhRate;
   @JsonProperty(value = "off_peak_kwh_rate")
   private Double offPeakKwhRate;
   @JsonProperty(value = "peak_begin")
   private Double peakBegin;
   @JsonProperty(value = "peak_end")
   private Double peakEnd;
   @JsonProperty(value = "co2_factor")
   private Double co2Factor;
   @JsonProperty(value = "cooling_factor")
   private Double coolingFactor;
   @JsonProperty(value = "custom_field_1")
   private String customField1;
   @JsonProperty(value = "custom_field_2")
   private String customField2;
   @JsonProperty(value = "external_key")
   private String external_key;
   private Double capacity;
   @JsonProperty(value = "cooling_savings")
   private Double coolingSavings;
   @JsonProperty(value = "pue_threshold_minimum")
   private String pueThresholdMinimum;
   @JsonProperty(value = "pue_threshold_maximum")
   private String pueThresholdMaximum;
   private Parent parent;

   public void setId(long id) {
      this.id = id;
   }

   public long getId() {
      return id;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public String getCompanyName() {
      return companyName;
   }

   public void setCompanyName(String companyName) {
      this.companyName = companyName;
   }

   public String getContactName() {
      return contactName;
   }

   public void setContactName(String contactName) {
      this.contactName = contactName;
   }

   public String getContactPhone() {
      return contactPhone;
   }

   public void setContactPhone(String contactPhone) {
      this.contactPhone = contactPhone;
   }

   public String getContactEmail() {
      return contactEmail;
   }

   public void setContactEmail(String contactEmail) {
      this.contactEmail = contactEmail;
   }

   public String getCity() {
      return city;
   }

   public void setCity(String city) {
      this.city = city;
   }

   public String getState() {
      return state;
   }

   public void setState(String state) {
      this.state = state;
   }

   public String getCountry() {
      return country;
   }

   public void setCountry(String country) {
      this.country = country;
   }

   public Double getPeakKwhRate() {
      return peakKwhRate;
   }

   public void setPeakKwhRate(Double peakKwhRate) {
      this.peakKwhRate = peakKwhRate;
   }

   public Double getOffPeakKwhRate() {
      return offPeakKwhRate;
   }

   public void setOffPeakKwhRate(Double offPeakKwhRate) {
      this.offPeakKwhRate = offPeakKwhRate;
   }

   public Double getPeakBegin() {
      return peakBegin;
   }

   public void setPeakBegin(Double peakBegin) {
      this.peakBegin = peakBegin;
   }

   public Double getPeakEnd() {
      return peakEnd;
   }

   public void setPeakEnd(Double peakEnd) {
      this.peakEnd = peakEnd;
   }

   public Double getCo2Factor() {
      return co2Factor;
   }

   public void setCo2Factor(Double co2Factor) {
      this.co2Factor = co2Factor;
   }

   public Double getCoolingFactor() {
      return coolingFactor;
   }

   public void setCoolingFactor(Double coolingFactor) {
      this.coolingFactor = coolingFactor;
   }

   public String getCustomField1() {
      return customField1;
   }

   public void setCustomField1(String customField1) {
      this.customField1 = customField1;
   }

   public String getCustomField2() {
      return customField2;
   }

   public void setCustomField2(String customField2) {
      this.customField2 = customField2;
   }

   public String getExternal_key() {
      return external_key;
   }

   public void setExternal_key(String external_key) {
      this.external_key = external_key;
   }

   public Double getCapacity() {
      return capacity;
   }

   public void setCapacity(Double capacity) {
      this.capacity = capacity;
   }

   public Double getCoolingSavings() {
      return coolingSavings;
   }

   public void setCoolingSavings(Double coolingSavings) {
      this.coolingSavings = coolingSavings;
   }

   public String getPueThresholdMinimum() {
      return pueThresholdMinimum;
   }

   public void setPueThresholdMinimum(String pueThresholdMinimum) {
      this.pueThresholdMinimum = pueThresholdMinimum;
   }

   public String getPueThresholdMaximum() {
      return pueThresholdMaximum;
   }

   public void setPueThresholdMaximum(String pueThresholdMaximum) {
      this.pueThresholdMaximum = pueThresholdMaximum;
   }

   public Parent getParent() {
      return parent;
   }

   public void setParent(Parent parent) {
      this.parent = parent;
   }

}