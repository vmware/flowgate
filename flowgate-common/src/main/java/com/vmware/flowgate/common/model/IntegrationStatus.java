/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class IntegrationStatus {

   private Status status;
   private String detail;
   private int retryCounter;
   
   public enum Status {
      ACTIVE, WARNING, PENDING, ERROR
   }

   public Status getStatus() {
      return status;
   }

   public void setStatus(Status status) {
      this.status = status;
   }

   public String getDetail() {
      return detail;
   }

   public void setDetail(String detail) {
      this.detail = detail;
   }

   public int getRetryCounter() {
      return retryCounter;
   }

   public void setRetryCounter(int retryCounter) {
      this.retryCounter = retryCounter;
   }

}
