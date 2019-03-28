/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.util.Date;

public class Authority {
   /**
    * authorityid
    */
   public String id;
   /**
    * authContent
    */
   private String authContent;
   /**
    * authUrl
    */
   private String authUrl;
   /**
    * 1 is manager resource,2 is APiUser resource
    */
   private String type;
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
   public String getType() {
      return type;
   }
   public void setType(String type) {
      this.type = type;
   }
   public Date getCreateTime() {
      return createTime;
   }
   public void setCreateTime(Date createTime) {
      this.createTime = createTime;
   }
   
}
