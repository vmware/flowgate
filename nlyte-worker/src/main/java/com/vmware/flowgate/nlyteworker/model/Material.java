/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetSubCategory;

public class Material {

   @JsonProperty(value = "@odata.type")
   private String odataType;
   @JsonProperty(value = "MaterialID")
   private int materialID;
   @JsonProperty(value = "MaterialNumber")
   private long materialNumber;
   @JsonProperty(value = "MaterialName")
   private String materialName;
   @JsonProperty(value = "MaterialTypeID")
   private int materialTypeID;
   @JsonProperty(value = "ManufacturerID")
   private int manufacturerID;
   @JsonProperty(value = "Model")
   private String model;
   @JsonProperty(value = "ExternalWidth")
   private double externalWidth;
   @JsonProperty(value = "ExternalDepth")
   private double externalDepth;
   @JsonProperty(value = "ExternalHeight")
   private double externalHeight;
   @JsonProperty(value = "Weight")
   private double weight;
   @JsonProperty(value = "RecordStatus")
   private String recordStatus;
   @JsonProperty(value = "IsEditable")
   private boolean isEditable;
   @JsonProperty(value="UHeight")
   private Integer uHeight;
   @JsonProperty(value = "TotalCopperPorts")
   private int totalCopperPorts;
   @JsonProperty(value = "TotalFibreOpticPorts")
   private int totalFibreOpticPorts;
   @JsonProperty(value = "TotalUndefinedPorts")
   private int totalUndefinedPorts;
   @JsonProperty(value = "NetworkSpeedMbps")
   private int networkSpeedMbps;
   @JsonProperty(value = "MountingTypeID")
   private String mountingTypeID;
   @JsonProperty(value = "TotalPlugs")
   private int totalPlugs;
   @JsonProperty(value = "MaterialSubtypeID")
   private long  materialSubtypeID;
   @JsonProperty(value = "RequiredPlugs")
   private int requiredPlugs;
   @JsonProperty(value = "RequiresDiversePower")
   private boolean requiresDiversePower;
   @JsonProperty(value = "AirflowTypeID")
   private String airflowTypeID;
   @JsonProperty(value = "PowerTypeID")
   private String powerTypeID;
   @JsonProperty(value = "MaterialCategoryID")
   private String materialCategoryID;
   @JsonProperty(value = "WorkableFootPrintFront")
   private double workableFootPrintFront;
   @JsonProperty(value = "WorkableFootPrintRear")
   private double workableFootPrintRear;
   @JsonProperty(value = "WorkableFootPrintHeight")
   private double workableFootPrintHeight;
   @JsonProperty(value = "PublishedPowerConsumption")
   private double publishedPowerConsumption;
   @JsonProperty(value = "MpafOverride")
   private String mpafOverride;
   @JsonProperty(value = "CalculatedMpafPowerConsumption")
   private double calculatedMpafPowerConsumption;
   @JsonProperty(value = "ConsumerPowerFactor")
   private double consumerPowerFactor;
   private AssetCategory materialType;
   private AssetSubCategory materialSubtype;
   @JsonProperty(value = "MaxA")
   private Double maxA;
   @JsonProperty(value = "MaxV")
   private Double maxV;
   @JsonProperty(value = "NumberOfColumnsBack")
   private Integer numberOfColumnsBack;
   @JsonProperty(value = "NumberOfColumnsFront")
   private Integer numberOfColumnsFront;
   @JsonProperty(value = "NumberOfRowsBack")
   private Integer numberOfRowsBack;
   @JsonProperty(value = "NumberOfRowsFront")
   private Integer numberOfRowsFront;

   public String getOdataType() {
      return odataType;
   }
   public void setOdataType(String odataType) {
      this.odataType = odataType;
   }
   public int getMaterialID() {
      return materialID;
   }
   public void setMaterialID(int materialID) {
      this.materialID = materialID;
   }
   public long getMaterialNumber() {
      return materialNumber;
   }
   public void setMaterialNumber(long materialNumber) {
      this.materialNumber = materialNumber;
   }
   public String getMaterialName() {
      return materialName;
   }
   public void setMaterialName(String materialName) {
      this.materialName = materialName;
   }
   public int getMaterialTypeID() {
      return materialTypeID;
   }
   public void setMaterialTypeID(int materialTypeID) {
      this.materialTypeID = materialTypeID;
   }
   public int getManufacturerID() {
      return manufacturerID;
   }
   public void setManufacturerID(int manufacturerID) {
      this.manufacturerID = manufacturerID;
   }
   public String getModel() {
      return model;
   }
   public void setModel(String model) {
      this.model = model;
   }
   public double getExternalWidth() {
      return externalWidth;
   }
   public void setExternalWidth(double externalWidth) {
      this.externalWidth = externalWidth;
   }
   public double getExternalDepth() {
      return externalDepth;
   }
   public void setExternalDepth(double externalDepth) {
      this.externalDepth = externalDepth;
   }
   public double getExternalHeight() {
      return externalHeight;
   }
   public void setExternalHeight(double externalHeight) {
      this.externalHeight = externalHeight;
   }
   public double getWeight() {
      return weight;
   }
   public void setWeight(double weight) {
      this.weight = weight;
   }
   public String getRecordStatus() {
      return recordStatus;
   }
   public void setRecordStatus(String recordStatus) {
      this.recordStatus = recordStatus;
   }
   public boolean isEditable() {
      return isEditable;
   }
   public void setEditable(boolean isEditable) {
      this.isEditable = isEditable;
   }
   public int getTotalCopperPorts() {
      return totalCopperPorts;
   }
   public void setTotalCopperPorts(int totalCopperPorts) {
      this.totalCopperPorts = totalCopperPorts;
   }
   public int getTotalFibreOpticPorts() {
      return totalFibreOpticPorts;
   }
   public void setTotalFibreOpticPorts(int totalFibreOpticPorts) {
      this.totalFibreOpticPorts = totalFibreOpticPorts;
   }
   public int getTotalUndefinedPorts() {
      return totalUndefinedPorts;
   }
   public void setTotalUndefinedPorts(int totalUndefinedPorts) {
      this.totalUndefinedPorts = totalUndefinedPorts;
   }
   public int getNetworkSpeedMbps() {
      return networkSpeedMbps;
   }
   public void setNetworkSpeedMbps(int networkSpeedMbps) {
      this.networkSpeedMbps = networkSpeedMbps;
   }
   public String getMountingTypeID() {
      return mountingTypeID;
   }
   public void setMountingTypeID(String mountingTypeID) {
      this.mountingTypeID = mountingTypeID;
   }
   public int getTotalPlugs() {
      return totalPlugs;
   }
   public void setTotalPlugs(int totalPlugs) {
      this.totalPlugs = totalPlugs;
   }
   public long getMaterialSubtypeID() {
      return materialSubtypeID;
   }
   public void setMaterialSubtypeID(long materialSubtypeID) {
      this.materialSubtypeID = materialSubtypeID;
   }
   public int getRequiredPlugs() {
      return requiredPlugs;
   }
   public void setRequiredPlugs(int requiredPlugs) {
      this.requiredPlugs = requiredPlugs;
   }
   public boolean isRequiresDiversePower() {
      return requiresDiversePower;
   }
   public void setRequiresDiversePower(boolean requiresDiversePower) {
      this.requiresDiversePower = requiresDiversePower;
   }
   public String getAirflowTypeID() {
      return airflowTypeID;
   }
   public void setAirflowTypeID(String airflowTypeID) {
      this.airflowTypeID = airflowTypeID;
   }
   public String getPowerTypeID() {
      return powerTypeID;
   }
   public void setPowerTypeID(String powerTypeID) {
      this.powerTypeID = powerTypeID;
   }
   public String getMaterialCategoryID() {
      return materialCategoryID;
   }
   public void setMaterialCategoryID(String materialCategoryID) {
      this.materialCategoryID = materialCategoryID;
   }
   public double getWorkableFootPrintFront() {
      return workableFootPrintFront;
   }
   public void setWorkableFootPrintFront(double workableFootPrintFront) {
      this.workableFootPrintFront = workableFootPrintFront;
   }
   public double getWorkableFootPrintRear() {
      return workableFootPrintRear;
   }
   public void setWorkableFootPrintRear(double workableFootPrintRear) {
      this.workableFootPrintRear = workableFootPrintRear;
   }
   public double getWorkableFootPrintHeight() {
      return workableFootPrintHeight;
   }
   public void setWorkableFootPrintHeight(double workableFootPrintHeight) {
      this.workableFootPrintHeight = workableFootPrintHeight;
   }
   public double getPublishedPowerConsumption() {
      return publishedPowerConsumption;
   }
   public void setPublishedPowerConsumption(double publishedPowerConsumption) {
      this.publishedPowerConsumption = publishedPowerConsumption;
   }
   public String getMpafOverride() {
      return mpafOverride;
   }
   public void setMpafOverride(String mpafOverride) {
      this.mpafOverride = mpafOverride;
   }
   public double getCalculatedMpafPowerConsumption() {
      return calculatedMpafPowerConsumption;
   }
   public void setCalculatedMpafPowerConsumption(double calculatedMpafPowerConsumption) {
      this.calculatedMpafPowerConsumption = calculatedMpafPowerConsumption;
   }
   public double getConsumerPowerFactor() {
      return consumerPowerFactor;
   }
   public void setConsumerPowerFactor(double consumerPowerFactor) {
      this.consumerPowerFactor = consumerPowerFactor;
   }
   public AssetCategory getMaterialType() {
      return materialType;
   }
   public void setMaterialType(AssetCategory materialType) {
      this.materialType = materialType;
   }
   public AssetSubCategory getMaterialSubtype() {
      return materialSubtype;
   }
   public void setMaterialSubtype(AssetSubCategory materialSubtype) {
      this.materialSubtype = materialSubtype;
   }
   public Integer getuHeight() {
      return uHeight;
   }
   public void setuHeight(Integer uHeight) {
      this.uHeight = uHeight;
   }
   public Double getMaxA() {
      return maxA;
   }
   public void setMaxA(Double maxA) {
      this.maxA = maxA;
   }
   public Double getMaxV() {
      return maxV;
   }
   public void setMaxV(Double maxV) {
      this.maxV = maxV;
   }
   public Integer getNumberOfColumnsBack() {
      return numberOfColumnsBack;
   }
   public void setNumberOfColumnsBack(Integer numberOfColumnsBack) {
      this.numberOfColumnsBack = numberOfColumnsBack;
   }
   public Integer getNumberOfColumnsFront() {
      return numberOfColumnsFront;
   }
   public void setNumberOfColumnsFront(Integer numberOfColumnsFront) {
      this.numberOfColumnsFront = numberOfColumnsFront;
   }
   public Integer getNumberOfRowsBack() {
      return numberOfRowsBack;
   }
   public void setNumberOfRowsBack(Integer numberOfRowsBack) {
      this.numberOfRowsBack = numberOfRowsBack;
   }
   public Integer getNumberOfRowsFront() {
      return numberOfRowsFront;
   }
   public void setNumberOfRowsFront(Integer numberOfRowsFront) {
      this.numberOfRowsFront = numberOfRowsFront;
   }

}
