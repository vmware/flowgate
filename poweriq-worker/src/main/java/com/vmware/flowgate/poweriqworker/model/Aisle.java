/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Aisle {

   private long id;
   private String name;
   @JsonProperty(value = "external_key")
   private String externalKey;
   private Double capacity;
   private Parent parent;

   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getExternalKey() {
      return externalKey;
   }

   public void setExternalKey(String externalKey) {
      this.externalKey = externalKey;
   }

   public Double getCapacity() {
      return capacity;
   }

   public void setCapacity(Double capacity) {
      this.capacity = capacity;
   }

   public Parent getParent() {
      return parent;
   }

   public void setParent(Parent parent) {
      this.parent = parent;
   }

}
