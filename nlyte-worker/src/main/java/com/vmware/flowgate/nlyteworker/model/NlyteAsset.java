/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NlyteAsset {

   @JsonProperty(value="AssetID")
   private int assetID;
   @JsonProperty(value="GridReferenceLayer")
   private String gridReferenceLayer;
   @JsonProperty(value="PowerState")
   private String powerState;
   @JsonProperty(value="AssetNumber")
   private int assetNumber;
   @JsonProperty(value="MaterialID")
   private int materialID;
   @JsonProperty(value="UMountingID")
   private String uMountingID;
   @JsonProperty(value="CabinetAssetID")
   private int cabinetAssetID;
   @JsonProperty(value="AssetName")
   private String assetName;
   @JsonProperty(value="PurchasePrice")
   private double purchasePrice;
   @JsonProperty(value="SerialNumber")
   private String serialNumber;
   @JsonProperty(value="Tag")
   private String tag;
   @JsonProperty(value="CreationDate")
   private String creationDate;
   @JsonProperty(value="OperationalDate")
   private String operationalDate;
   @JsonProperty(value="OperationalStatus")
   private String operationalStatus;
   @JsonProperty(value="LocationGroupID")
   private int locationGroupID;
   @JsonProperty(value="LocationGroupOrChildrenID")
   private int locationGroupOrChildrenID;
   @JsonProperty(value="RecordStatus")
   private String recordStatus;
   @JsonProperty(value="SubStatusID")
   private String subStatusID;
   @JsonProperty(value="TemplateID")
   private String templateID;
   @JsonProperty(value="IsTemplateRelated")
   private boolean isTemplateRelated;
   @JsonProperty(value="GridReference")
   private String gridReference;
   @JsonProperty(value="GridReferenceRow")
   private String gridReferenceRow;
   @JsonProperty(value="GridReferenceColumn")
   private String gridReferenceColumn;
   @JsonProperty(value="UniqueAssetName")
   private String uniqueAssetName;
   @JsonProperty(value="CostCenter")
   private String costCenter;
   @JsonProperty(value="DamagedRFIDTag")
   private String damagedRFIDTag;
   @JsonProperty(value="PONumber")
   private String pONumber;
   @JsonProperty(value="PRNumber")
   private String pRNumber;
   @JsonProperty(value="RFIDTag")
   private String rFIDTag;
   @JsonProperty(value="SAPAssetNumber")
   private String sAPAssetNumber;
   @JsonProperty(value="PurchaseDate")
   private String purchaseDate;
   @JsonProperty(value="IsStaticSwitchEnabled")
   private boolean isStaticSwitchEnabled;
   @JsonProperty(value="HardwareSupportWeighting")
   private double hardwareSupportWeighting;
   @JsonProperty(value="TrancheNumber")
   private int trancheNumber;
   @JsonProperty(value="TotalSystemSpace")
   private int totalSystemSpace;
   @JsonProperty(value="StrategyID")
   private String strategyID;
   @JsonProperty(value="ProjectID")
   private String projectID;
   @JsonProperty(value="ChassisMountedAssetMapID")
   private int chassisMountedAssetMapID;
   @JsonProperty(value="AuditDate")
   private String auditDate;
   @JsonProperty(value="AuditStatusCorrect")
   private String auditStatusCorrect;
   @JsonProperty(value="LastAuditorAccountID")
   private String lastAuditorAccountID;
   @JsonProperty(value="TotalPlugs")
   private int totalPlugs;
   @JsonProperty(value="UMounting")
   private UMounting uMounting;
   private String cabinetName;
   @JsonProperty(value="ContiguousUSpace")
   private String contiguousUSpace;
   @JsonProperty(value="CabinetUs")
   private List<CabinetU> cabinetUs;

   @JsonProperty(value = "CustomFields")
   private List<CustomField> customFields;
   @JsonProperty(value = "ChassisSlots")
   private List<ChassisSlot> chassisSlots;
   @JsonProperty(value = "ChassisMountedAssetMaps")
   private List<ChassisMountedAssetMap> chassisMountedAssetMaps;

   public String getCabinetName() {
      return cabinetName;
   }
   public void setCabinetName(String cabinetName) {
      this.cabinetName = cabinetName;
   }
   public boolean isActived() {
      return "Active".equals(recordStatus) && "Operational".equals(operationalStatus);
   }
   public int getAssetID() {
      return assetID;
   }
   public void setAssetID(int assetID) {
      this.assetID = assetID;
   }
   public String getGridReferenceLayer() {
      return gridReferenceLayer;
   }
   public void setGridReferenceLayer(String gridReferenceLayer) {
      this.gridReferenceLayer = gridReferenceLayer;
   }
   public String getPowerState() {
      return powerState;
   }
   public void setPowerState(String powerState) {
      this.powerState = powerState;
   }
   public int getAssetNumber() {
      return assetNumber;
   }
   public void setAssetNumber(int assetNumber) {
      this.assetNumber = assetNumber;
   }
   public int getMaterialID() {
      return materialID;
   }
   public void setMaterialID(int materialID) {
      this.materialID = materialID;
   }
   public String getuMountingID() {
      return uMountingID;
   }
   public void setuMountingID(String uMountingID) {
      this.uMountingID = uMountingID;
   }
   public int getCabinetAssetID() {
      return cabinetAssetID;
   }
   public void setCabinetAssetID(int cabinetAssetID) {
      this.cabinetAssetID = cabinetAssetID;
   }
   public String getAssetName() {
      return assetName;
   }
   public void setAssetName(String assetName) {
      this.assetName = assetName;
   }
   public double getPurchasePrice() {
      return purchasePrice;
   }
   public void setPurchasePrice(double purchasePrice) {
      this.purchasePrice = purchasePrice;
   }
   public String getSerialNumber() {
      return serialNumber;
   }
   public void setSerialNumber(String serialNumber) {
      this.serialNumber = serialNumber;
   }
   public String getTag() {
      return tag;
   }
   public void setTag(String tag) {
      this.tag = tag;
   }
   public String getCreationDate() {
      return creationDate;
   }
   public void setCreationDate(String creationDate) {
      this.creationDate = creationDate;
   }
   public String getOperationalDate() {
      return operationalDate;
   }
   public void setOperationalDate(String operationalDate) {
      this.operationalDate = operationalDate;
   }
   public String getOperationalStatus() {
      return operationalStatus;
   }
   public void setOperationalStatus(String operationalStatus) {
      this.operationalStatus = operationalStatus;
   }
   public int getLocationGroupID() {
      return locationGroupID;
   }
   public void setLocationGroupID(int locationGroupID) {
      this.locationGroupID = locationGroupID;
   }
   public int getLocationGroupOrChildrenID() {
      return locationGroupOrChildrenID;
   }
   public void setLocationGroupOrChildrenID(int locationGroupOrChildrenID) {
      this.locationGroupOrChildrenID = locationGroupOrChildrenID;
   }
   public String getRecordStatus() {
      return recordStatus;
   }
   public void setRecordStatus(String recordStatus) {
      this.recordStatus = recordStatus;
   }
   public String getSubStatusID() {
      return subStatusID;
   }
   public void setSubStatusID(String subStatusID) {
      this.subStatusID = subStatusID;
   }
   public String getTemplateID() {
      return templateID;
   }
   public void setTemplateID(String templateID) {
      this.templateID = templateID;
   }
   public boolean isTemplateRelated() {
      return isTemplateRelated;
   }
   public void setTemplateRelated(boolean isTemplateRelated) {
      this.isTemplateRelated = isTemplateRelated;
   }
   public String getGridReference() {
      return gridReference;
   }
   public void setGridReference(String gridReference) {
      this.gridReference = gridReference;
   }
   public String getGridReferenceRow() {
      return gridReferenceRow;
   }
   public void setGridReferenceRow(String gridReferenceRow) {
      this.gridReferenceRow = gridReferenceRow;
   }
   public String getGridReferenceColumn() {
      return gridReferenceColumn;
   }
   public void setGridReferenceColumn(String gridReferenceColumn) {
      this.gridReferenceColumn = gridReferenceColumn;
   }
   public String getUniqueAssetName() {
      return uniqueAssetName;
   }
   public void setUniqueAssetName(String uniqueAssetName) {
      this.uniqueAssetName = uniqueAssetName;
   }
   public String getCostCenter() {
      return costCenter;
   }
   public void setCostCenter(String costCenter) {
      this.costCenter = costCenter;
   }
   public String getDamagedRFIDTag() {
      return damagedRFIDTag;
   }
   public void setDamagedRFIDTag(String damagedRFIDTag) {
      this.damagedRFIDTag = damagedRFIDTag;
   }
   public String getpONumber() {
      return pONumber;
   }
   public void setpONumber(String pONumber) {
      this.pONumber = pONumber;
   }
   public String getpRNumber() {
      return pRNumber;
   }
   public void setpRNumber(String pRNumber) {
      this.pRNumber = pRNumber;
   }
   public String getrFIDTag() {
      return rFIDTag;
   }
   public void setrFIDTag(String rFIDTag) {
      this.rFIDTag = rFIDTag;
   }
   public String getsAPAssetNumber() {
      return sAPAssetNumber;
   }
   public void setsAPAssetNumber(String sAPAssetNumber) {
      this.sAPAssetNumber = sAPAssetNumber;
   }
   public String getPurchaseDate() {
      return purchaseDate;
   }
   public void setPurchaseDate(String purchaseDate) {
      this.purchaseDate = purchaseDate;
   }
   public boolean isStaticSwitchEnabled() {
      return isStaticSwitchEnabled;
   }
   public void setStaticSwitchEnabled(boolean isStaticSwitchEnabled) {
      this.isStaticSwitchEnabled = isStaticSwitchEnabled;
   }
   public double getHardwareSupportWeighting() {
      return hardwareSupportWeighting;
   }
   public void setHardwareSupportWeighting(double hardwareSupportWeighting) {
      this.hardwareSupportWeighting = hardwareSupportWeighting;
   }
   public int getTrancheNumber() {
      return trancheNumber;
   }
   public void setTrancheNumber(int trancheNumber) {
      this.trancheNumber = trancheNumber;
   }
   public int getTotalSystemSpace() {
      return totalSystemSpace;
   }
   public void setTotalSystemSpace(int totalSystemSpace) {
      this.totalSystemSpace = totalSystemSpace;
   }
   public String getStrategyID() {
      return strategyID;
   }
   public void setStrategyID(String strategyID) {
      this.strategyID = strategyID;
   }
   public String getProjectID() {
      return projectID;
   }
   public void setProjectID(String projectID) {
      this.projectID = projectID;
   }
   public int getChassisMountedAssetMapID() {
      return chassisMountedAssetMapID;
   }
   public void setChassisMountedAssetMapID(int chassisMountedAssetMapID) {
      this.chassisMountedAssetMapID = chassisMountedAssetMapID;
   }
   public String getAuditDate() {
      return auditDate;
   }
   public void setAuditDate(String auditDate) {
      this.auditDate = auditDate;
   }
   public String getAuditStatusCorrect() {
      return auditStatusCorrect;
   }
   public void setAuditStatusCorrect(String auditStatusCorrect) {
      this.auditStatusCorrect = auditStatusCorrect;
   }
   public String getLastAuditorAccountID() {
      return lastAuditorAccountID;
   }
   public void setLastAuditorAccountID(String lastAuditorAccountID) {
      this.lastAuditorAccountID = lastAuditorAccountID;
   }
   public int getTotalPlugs() {
      return totalPlugs;
   }
   public void setTotalPlugs(int totalPlugs) {
      this.totalPlugs = totalPlugs;
   }
   public UMounting getuMounting() {
      return uMounting;
   }
   public void setuMounting(UMounting uMounting) {
      this.uMounting = uMounting;
   }
   public String getContiguousUSpace() {
      return contiguousUSpace;
   }
   public void setContiguousUSpace(String contiguousUSpace) {
      this.contiguousUSpace = contiguousUSpace;
   }
   public List<CabinetU> getCabinetUs() {
      return cabinetUs;
   }
   public void setCabinetUs(List<CabinetU> cabinetUs) {
      this.cabinetUs = cabinetUs;
   }

   /**
    * @return the customFields
    */
   public List<CustomField> getCustomFields() {
      return customFields;
   }

   /**
    * @param customFields
    *           the customFields to set
    */
   public void setCustomFields(List<CustomField> customFields) {
      this.customFields = customFields;
   }
   public List<ChassisSlot> getChassisSlots() {
      return chassisSlots;
   }
   public void setChassisSlots(List<ChassisSlot> chassisSlots) {
      this.chassisSlots = chassisSlots;
   }
   public List<ChassisMountedAssetMap> getChassisMountedAssetMaps() {
      return chassisMountedAssetMaps;
   }
   public void setChassisMountedAssetMaps(List<ChassisMountedAssetMap> chassisMountedAssetMaps) {
      this.chassisMountedAssetMaps = chassisMountedAssetMaps;
   }

}
