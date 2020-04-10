/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomField {

   public static final String Tenant_Manager = "Tenant_Manager";
   public static final String Tenant_EndUser = "Tenant_EndUser";
   public static final String Owner = "Owner";
   public static final String HaaS_RequestedBy = "HaaS_RequestedBy";
   @JsonProperty(value = "DataFieldAssetValueID")
   private int dataFieldAssetValueID;

   @JsonProperty(value = "AssetID")
   private int assetID;

   @JsonProperty(value = "DataLabel")
   private String dataLabel;

   @JsonProperty(value = "DataValueCheckBox")
   private String dataValueCheckBox;

   @JsonProperty(value = "DataValueDate")
   private String dataValueDate;

   @JsonProperty(value = "DataValueString")
   private String dataValueString;

   /**
    * @return the dataFieldAssetValueID
    */
   public int getDataFieldAssetValueID() {
      return dataFieldAssetValueID;
   }

   /**
    * @param dataFieldAssetValueID
    *           the dataFieldAssetValueID to set
    */
   public void setDataFieldAssetValueID(int dataFieldAssetValueID) {
      this.dataFieldAssetValueID = dataFieldAssetValueID;
   }

   /**
    * @return the assetID
    */
   public int getAssetID() {
      return assetID;
   }

   /**
    * @param assetID
    *           the assetID to set
    */
   public void setAssetID(int assetID) {
      this.assetID = assetID;
   }

   /**
    * @return the dataLabel
    */
   public String getDataLabel() {
      return dataLabel;
   }

   /**
    * @param dataLabel
    *           the dataLabel to set
    */
   public void setDataLabel(String dataLabel) {
      this.dataLabel = dataLabel;
   }

   /**
    * @return the dataValueCheckBox
    */
   public String getDataValueCheckBox() {
      return dataValueCheckBox;
   }

   /**
    * @param dataValueCheckBox
    *           the dataValueCheckBox to set
    */
   public void setDataValueCheckBox(String dataValueCheckBox) {
      this.dataValueCheckBox = dataValueCheckBox;
   }

   /**
    * @return the dataValueDate
    */
   public String getDataValueDate() {
      return dataValueDate;
   }

   /**
    * @param dataValueDate
    *           the dataValueDate to set
    */
   public void setDataValueDate(String dataValueDate) {
      this.dataValueDate = dataValueDate;
   }

   /**
    * @return the dataValueString
    */
   public String getDataValueString() {
      return dataValueString;
   }

   /**
    * @param dataValueString
    *           the dataValueString to set
    */
   public void setDataValueString(String dataValueString) {
      this.dataValueString = dataValueString;
   }


}
