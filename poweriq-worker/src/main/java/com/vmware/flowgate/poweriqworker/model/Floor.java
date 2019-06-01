/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Floor {

   private long id;
   private String name;
   @JsonProperty(value = "external_key")
   private String externalKey;
   private Double capacity;
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

   public String getExternalKey() {
      return externalKey;
   }

   public void setExternalKey(String externalKey) {
      this.externalKey = externalKey;
   }

   public void setCapacity(Double capacity) {
      this.capacity = capacity;
   }

   public Double getCapacity() {
      return capacity;
   }

   public void setParent(Parent parent) {
      this.parent = parent;
   }

   public Parent getParent() {
      return parent;
   }

}