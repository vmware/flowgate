/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;

public class WormholeUser implements BaseDocument {
   /**
    * userId
    */
   @Id
   private String id;
   /**
    * userName
    */
   private String userName;
   /**
    * gender
    */
   private Integer gender;
   /**
    * password
    */
   private String password;
   /**
    * mobile
    */
   private String mobile;
   /**
    * status ：1available，0Unavailable
    */
   private Integer status;
   /**
    * createTime
    */
   private Date createTime;
   /**
    * email
    */
   private String emailAddress;

   private List<String> roleNames;

   private List<String> userGroupIDs;

   private long lastPasswordResetDate;


   public List<String> getRoleNames() {
      return roleNames;
   }

   public void setRoleNames(List<String> roleNames) {
      this.roleNames = roleNames;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }
   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public Integer getGender() {
      return gender;
   }

   public void setGender(Integer gender) {
      this.gender = gender;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getMobile() {
      return mobile;
   }

   public void setMobile(String mobile) {
      this.mobile = mobile;
   }

   public Integer getStatus() {
      return status;
   }

   public void setStatus(Integer status) {
      this.status = status;
   }

   public Date getCreateTime() {
      return createTime;
   }

   public void setCreateTime(Date createTime) {
      this.createTime = createTime;
   }

   public String getEmailAddress() {
      return emailAddress;
   }

   public void setEmailAddress(String emailAddress) {
      this.emailAddress = emailAddress;
   }

   public long getLastPasswordResetDate() {
      return lastPasswordResetDate;
   }

   public void setLastPasswordResetDate(long lastPasswordResetDate) {
      this.lastPasswordResetDate = lastPasswordResetDate;
   }

   public List<String> getUserGroupIDs() {
      return userGroupIDs;
   }

   public void setUserGroupIDs(List<String> userGroupIDs) {
      this.userGroupIDs = userGroupIDs;
   }

}
