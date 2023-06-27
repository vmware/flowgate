/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import org.springframework.data.annotation.Id;

public class ServerMapping implements BaseDocument {
   @Id
   private String id;
   private String asset;
   private String vcID;
   private String vcHostName;
   private String vcMobID;
   private String vcClusterMobID;
   private String vcInstanceUUID;
   private String vroID;
   private String vroResourceName;
   private String vroVMEntityName;
   private String vroVMEntityObjectID;
   private String vroVMEntityVCID;
   private String vroResourceID;

   public String getAsset() {
      return asset;
   }

   public void setAsset(String asset) {
      this.asset = asset;
   }

   public String getVcID() {
      return vcID;
   }

   public void setVcID(String vcID) {
      this.vcID = vcID;
   }

   public String getVcHostName() {
      return vcHostName;
   }

   public void setVcHostName(String vcHostName) {
      this.vcHostName = vcHostName;
   }

   public String getVcMobID() {
      return vcMobID;
   }

   public void setVcMobID(String vcMobID) {
      this.vcMobID = vcMobID;
   }

   public String getVroID() {
      return vroID;
   }

   public void setVroID(String vroID) {
      this.vroID = vroID;
   }

   public String getVroResourceName() {
      return vroResourceName;
   }

   public void setVroResourceName(String vroResourceName) {
      this.vroResourceName = vroResourceName;
   }

   public String getVroVMEntityName() {
      return vroVMEntityName;
   }

   public void setVroVMEntityName(String vroVMEntityName) {
      this.vroVMEntityName = vroVMEntityName;
   }

   public String getVroVMEntityObjectID() {
      return vroVMEntityObjectID;
   }

   public void setVroVMEntityObjectID(String vroVMEntityObjectID) {
      this.vroVMEntityObjectID = vroVMEntityObjectID;
   }

   public String getVroVMEntityVCID() {
      return vroVMEntityVCID;
   }

   public void setVroVMEntityVCID(String vroVMEntityVCID) {
      this.vroVMEntityVCID = vroVMEntityVCID;
   }

   public String getVroResourceID() {
      return vroResourceID;
   }

   public void setVroResourceID(String vroResourceID) {
      this.vroResourceID = vroResourceID;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getVcClusterMobID() {
      return vcClusterMobID;
   }

   public void setVcClusterMobID(String vcClusterMobID) {
      this.vcClusterMobID = vcClusterMobID;
   }

   public String getVcInstanceUUID() {
      return vcInstanceUUID;
   }

   public void setVcInstanceUUID(String vcInstanceUUID) {
      this.vcInstanceUUID = vcInstanceUUID;
   }

}
