/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class Tenant {
   private String owner;
   private String tenant;
   private String tenantManager;

   /**
    * @return the owner
    */
   public String getOwner() {
      return owner;
   }

   /**
    * @param owner
    *           the owner to set
    */
   public void setOwner(String owner) {
      this.owner = owner;
   }

   /**
    * @return the tenant
    */
   public String getTenant() {
      return tenant;
   }

   /**
    * @param tenant
    *           the tenant to set
    */
   public void setTenant(String tenant) {
      this.tenant = tenant;
   }

   /**
    * @return the tenantManager
    */
   public String getTenantManager() {
      return tenantManager;
   }

   /**
    * @param tenantManager
    *           the tenantManager to set
    */
   public void setTenantManager(String tenantManager) {
      this.tenantManager = tenantManager;
   }

}
