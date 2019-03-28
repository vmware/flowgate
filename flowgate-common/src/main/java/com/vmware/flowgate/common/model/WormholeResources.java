/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;


public class WormholeResources {
   private String id;
   private String pattern;
   private String httpMethod;
   private int sortWeight;
   
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   public String getPattern() {
      return pattern;
   }
   public void setPattern(String pattern) {
      this.pattern = pattern;
   }
   public String getHttpMethod() {
      return httpMethod;
   }
   public void setHttpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
   }
   public int getSortWeight() {
      return sortWeight;
   }
   public void setSortWeight(int sortWeight) {
      this.sortWeight = sortWeight;
   }
   
}
