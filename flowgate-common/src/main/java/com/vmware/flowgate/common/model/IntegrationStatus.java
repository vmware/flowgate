package com.vmware.flowgate.common.model;

public class IntegrationStatus {

   private Status status;
   private String detail;
   public enum Status{
      ACTIVE,PENDING,ERROR
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
   
}
