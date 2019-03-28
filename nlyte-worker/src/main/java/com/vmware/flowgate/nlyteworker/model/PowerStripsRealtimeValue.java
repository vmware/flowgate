/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PowerStripsRealtimeValue {

   @JsonProperty(value="AssetId")
   private long assetId;
   @JsonProperty(value="ConnectorNo")
   private int connectorNo;
   @JsonProperty(value="Name")
   private String name;
   @JsonProperty(value="Unit")
   private String unit;
   @JsonProperty(value="Value")
   private double value;
   @JsonProperty(value="Phase1Value")
   private double phase1Value;
   @JsonProperty(value="Phase2Value")
   private double Phase2Value;
   @JsonProperty(value="Phase3Value")
   private double Phase3Value;
   @JsonProperty(value="UnitMultiplyFactor")
   private int unitMultiplyFactor;
   @JsonProperty(value="RecordedDateTime")
   private String recordedDateTime;
   @JsonProperty(value="ThreePhaseRecordedDateTime")
   private String threePhaseRecordedDateTime;
   @JsonProperty(value="ProviderId")
   private int providerId;
   @JsonProperty(value="ProviderName")
   private String providerName;
   @JsonProperty(value="Position")
   private int position;
   @JsonProperty(value="ConnectorType")
   private String connectorType;
   public long getAssetId() {
      return assetId;
   }
   public void setAssetId(long assetId) {
      this.assetId = assetId;
   }
   public int getConnectorNo() {
      return connectorNo;
   }
   public void setConnectorNo(int connectorNo) {
      this.connectorNo = connectorNo;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getUnit() {
      return unit;
   }
   public void setUnit(String unit) {
      this.unit = unit;
   }
   public double getValue() {
      return value;
   }
   public void setValue(double value) {
      this.value = value;
   }
   public double getPhase1Value() {
      return phase1Value;
   }
   public void setPhase1Value(double phase1Value) {
      this.phase1Value = phase1Value;
   }
   public double getPhase2Value() {
      return Phase2Value;
   }
   public void setPhase2Value(double phase2Value) {
      Phase2Value = phase2Value;
   }
   public double getPhase3Value() {
      return Phase3Value;
   }
   public void setPhase3Value(double phase3Value) {
      Phase3Value = phase3Value;
   }
   public int getUnitMultiplyFactor() {
      return unitMultiplyFactor;
   }
   public void setUnitMultiplyFactor(int unitMultiplyFactor) {
      this.unitMultiplyFactor = unitMultiplyFactor;
   }
   public String getRecordedDateTime() {
      return recordedDateTime;
   }
   public void setRecordedDateTime(String recordedDateTime) {
      this.recordedDateTime = recordedDateTime;
   }
   public String getThreePhaseRecordedDateTime() {
      return threePhaseRecordedDateTime;
   }
   public void setThreePhaseRecordedDateTime(String threePhaseRecordedDateTime) {
      this.threePhaseRecordedDateTime = threePhaseRecordedDateTime;
   }
   public int getProviderId() {
      return providerId;
   }
   public void setProviderId(int providerId) {
      this.providerId = providerId;
   }
   public String getProviderName() {
      return providerName;
   }
   public void setProviderName(String providerName) {
      this.providerName = providerName;
   }
   public int getPosition() {
      return position;
   }
   public void setPosition(int position) {
      this.position = position;
   }
   public String getConnectorType() {
      return connectorType;
   }
   public void setConnectorType(String connectorType) {
      this.connectorType = connectorType;
   }
}
