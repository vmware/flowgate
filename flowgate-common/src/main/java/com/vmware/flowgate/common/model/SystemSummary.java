/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.io.Serializable;
import java.util.List;

public class SystemSummary implements Serializable {

   private static final long serialVersionUID = 1L;
   private int assetsNum = 0;
   private int facilitySystemNum = 0;

   private int serverNum = 0;
   private int pduNum = 0;
   private int cabinetNum = 0;
   private int switchNum = 0;
   private int sensorNum = 0;

   private int humiditySensorNum = 0;
   private int temperatureSensorNum = 0;
   private int airFlowSensorNum = 0;
   private int smokeSensorNum = 0;
   private int waterSensorNum = 0;

   private int userNum = 0;
   private int sddcServerNum = 0;
   private int sddcIntegrationNum = 0;
   private int vcNum = 0;
   private int vroNum = 0;
   private List<NlyteSummary> nlyteSummary;
   private List<PowerIqSummary> powerIqSummary;
   private List<VcSummary> vcSummary;
   private List<VroSummary> vroSummary;

   public int getAssetsNum() {
      return assetsNum;
   }

   public void setAssetsNum(int assetsNum) {
      this.assetsNum = assetsNum;
   }

   public int getFacilitySystemNum() {
      return facilitySystemNum;
   }

   public void setFacilitySystemNum(int facilitySystemNum) {
      this.facilitySystemNum = facilitySystemNum;
   }

   public int getServerNum() {
      return serverNum;
   }

   public void setServerNum(int serverNum) {
      this.serverNum = serverNum;
   }

   public int getPduNum() {
      return pduNum;
   }

   public void setPduNum(int pduNum) {
      this.pduNum = pduNum;
   }

   public int getCabinetNum() {
      return cabinetNum;
   }

   public void setCabinetNum(int cabinetNum) {
      this.cabinetNum = cabinetNum;
   }

   public int getSwitchNum() {
      return switchNum;
   }

   public void setSwitchNum(int switchNum) {
      this.switchNum = switchNum;
   }

   public int getSensorNum() {
      return sensorNum;
   }

   public void setSensorNum(int sensorNum) {
      this.sensorNum = sensorNum;
   }

   public int getHumiditySensorNum() {
      return humiditySensorNum;
   }

   public void setHumiditySensorNum(int humiditySensorNum) {
      this.humiditySensorNum = humiditySensorNum;
   }

   public int getTemperatureSensorNum() {
      return temperatureSensorNum;
   }

   public void setTemperatureSensorNum(int temperatureSensorNum) {
      this.temperatureSensorNum = temperatureSensorNum;
   }

   public int getAirFlowSensorNum() {
      return airFlowSensorNum;
   }

   public void setAirFlowSensorNum(int airFlowSensorNum) {
      this.airFlowSensorNum = airFlowSensorNum;
   }

   public int getSmokeSensorNum() {
      return smokeSensorNum;
   }

   public void setSmokeSensorNum(int smokeSensorNum) {
      this.smokeSensorNum = smokeSensorNum;
   }

   public int getWaterSensorNum() {
      return waterSensorNum;
   }

   public void setWaterSensorNum(int waterSensorNum) {
      this.waterSensorNum = waterSensorNum;
   }

   public int getUserNum() {
      return userNum;
   }

   public void setUserNum(int userNum) {
      this.userNum = userNum;
   }

   public int getSddcServerNum() {
      return sddcServerNum;
   }

   public void setSddcServerNum(int sddcServerNum) {
      this.sddcServerNum = sddcServerNum;
   }

   public int getSddcIntegrationNum() {
      return sddcIntegrationNum;
   }

   public void setSddcIntegrationNum(int sddcIntegrationNum) {
      this.sddcIntegrationNum = sddcIntegrationNum;
   }

   public int getVcNum() {
      return vcNum;
   }

   public void setVcNum(int vcNum) {
      this.vcNum = vcNum;
   }

   public int getVroNum() {
      return vroNum;
   }

   public void setVroNum(int vroNum) {
      this.vroNum = vroNum;
   }

   public List<NlyteSummary> getNlyteSummary() {
      return nlyteSummary;
   }

   public void setNlyteSummary(List<NlyteSummary> nlyteSummary) {
      this.nlyteSummary = nlyteSummary;
   }

   public List<PowerIqSummary> getPowerIqSummary() {
      return powerIqSummary;
   }

   public void setPowerIqSummary(List<PowerIqSummary> powerIqSummary) {
      this.powerIqSummary = powerIqSummary;
   }

   public List<VcSummary> getVcSummary() {
      return vcSummary;
   }

   public void setVcSummary(List<VcSummary> vcSummary) {
      this.vcSummary = vcSummary;
   }

   public List<VroSummary> getVroSummary() {
      return vroSummary;
   }

   public void setVroSummary(List<VroSummary> vroSummary) {
      this.vroSummary = vroSummary;
   }


}
