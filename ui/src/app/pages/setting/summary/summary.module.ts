import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

@NgModule({
  declarations: [],
  imports: [
    CommonModule
  ]
})
export class SummaryModule {
  assetsNum: number;
  facilitySystemNum: number;
  serverNum: number;
  pduNum: number;
  cabinetNum: number;
  switchNum: number;
  sensorNum: number;
  categoryIsUpsNum: number;
  userNum: number;
  sddcServerNum: number;
  sddcIntegrationNum: number;
  vcNum: number;
  vroNum: number;
  humiditySensorNum: number;
  temperatureSensorNum: number;
  airFlowSensorNum: number;
  smokeSensorNum: number;
  waterSensorNum: number;
  nlyteSummary: [];
  powerIqSummary: [];
  vcSummary: [];
  vroSummary: []
 }
