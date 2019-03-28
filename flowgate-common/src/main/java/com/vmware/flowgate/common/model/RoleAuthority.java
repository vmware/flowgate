/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.util.Date;

public class RoleAuthority {
   /**
    * id
    */
   private String id;
   /**
    * roleId
    */
   private String roleId;
   /**
    * authorityId
    */
   private String authorityId;
   /**
    * authContent
    */
   private String authContent;
   /**
    * authUrl
    */
   private String authUrl;
   /**
    * createTime
    */
   private Date createTime;

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getRoleId() {
      return roleId;
   }

   public void setRoleId(String roleId) {
      this.roleId = roleId;
   }

   public String getAuthContent() {
      return authContent;
   }

   public void setAuthContent(String authContent) {
      this.authContent = authContent;
   }

   public String getAuthUrl() {
      return authUrl;
   }

   public void setAuthUrl(String authUrl) {
      this.authUrl = authUrl;
   }

   public Date getCreateTime() {
      return createTime;
   }

   public void setCreateTime(Date createTime) {
      this.createTime = createTime;
   }

   public String getAuthorityId() {
      return authorityId;
   }

   public void setAuthorityId(String authorityId) {
      this.authorityId = authorityId;
   }
   
   
}
