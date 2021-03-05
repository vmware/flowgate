/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

@NgModule({
  imports: [
    CommonModule
  ],
  declarations: []
})
export class AssetModule {
  id:string;
  assetName:string;
  assetSource:string;
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
  cabinetUnitPosition:string;
  mountingSide:{};
  capacity:number;
  freeCapacity:number;
  cabinetAssetNumber:string;
  assetRealtimeDataSpec:string;
  Justificationfields:any;
  parent:any;
  status:any;
  pdus:string[];
  switches:string[];
  metricsformulars:any;
  enable:boolean=false;
 }
