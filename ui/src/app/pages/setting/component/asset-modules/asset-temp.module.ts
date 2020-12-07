/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AssetStatusModule } from './assetstatus.module';
import { AssetRealtimeDataSpecModule } from './assetrealtimedataspec.module';



@NgModule({
  declarations: [],
  imports: [
    CommonModule
  ]
})
export class AssetTempModule {
  id:string;
  assetNumber:number;
  assetName:string;
  assetSource:string;
  category:string;
  subCategory:string;
  manufacturer:string;
  serialnumber:string;
  model:string;
  tag:string;
  assetAddress:{};
  region:string;
  country:string;
  city:string;
  building:string;
  floor:string;
  room:string;
  row:string;
  col:string;
  extraLocation:string;
  cabinetName:string;
  cabinetUnitPosition:number;
  mountingSide:{};
  capacity:number;
  freeCapacity:number;
  cabinetAssetNumber:string;
  assetRealtimeDataSpec:AssetRealtimeDataSpecModule = new AssetRealtimeDataSpecModule();
  justificationfields:any;
  parent:any;
  status:AssetStatusModule = new AssetStatusModule();
  pdus:string[];
  switches:string[];
  metricsformulars:any;
  lastupdate:number;
  created:number;
  tenant:any;
  unit:string;
  validNumMin:any;
  validNumMax:any;
 }
