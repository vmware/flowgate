/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { AssetModule } from '../asset-modules/asset.module';
import {Router,ActivatedRoute} from '@angular/router';
import { SettingService } from '../../setting.service';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { AssetRealtimeDataSpecModule } from '../asset-modules/assetrealtimedataspec.module';
import { AssetStatusModule } from '../asset-modules/assetstatus.module';
@Component({
  selector: 'app-asset-add',
  templateUrl: './asset-add.component.html',
  styleUrls: ['./asset-add.component.scss']
})
export class AssetAddComponent implements OnInit {
  description:any;
  asset:AssetModule = new AssetModule();
  addfail:boolean =false;
  addassetloading:boolean = false;
  tip:string = " ";
  allcategory=[ "Server", "PDU","Cabinet","Networks","Sensors","UPS","Chassis"];
  suballcategory = {"Server":["Blade","Standard"], 
  "Sensors":["Humidity","Temperature","AirPressure","AirFlow","ContactClosure","Smoke","Water","Vibration"]};
  addAssetForm:FormGroup;
  constructor(private service:SettingService,private router:Router,private fb: FormBuilder) { 
    this.addAssetForm = this.fb.group({
      category: ['', [
        Validators.required
      ]],
      assetName: ['', [
        Validators.required
      ]],
      subCategory: ['', [
        Validators.required
      ]],
      mountingSide: ['', [
        Validators.required
      ]],
      assetNumber: ['', [
      ]],
      manufacturer: ['', [
      ]],
      model: ['', [
      ]],
      serialnumber: ['', [
      ]],
      tag: ['', [
      ]],
      cabinetName: ['', [
      ]],
      cabinetUnitPosition: ['', [
      ]],
      capacity: ['', [
      ]],
      cabinetAssetNumber: ['', [
      ]],
      region: ['', [
      ]],
      country: ['', [
      ]],
      city: ['', [
      ]],
      building: ['', [
      ]],
      floor: ['', [
      ]],
      room: ['', [
      ]],
      row: ['', [
      ]],
      col: ['', [
      ]],
      unit: ['', [
      ]],
      validNumMin: ['', [
      ]],
      validNumMax: ['', [
      ]],
      status: ['', [
        Validators.required
      ]]
    });
  }
  changeType(){
    let category:string = this.addAssetForm.get('category').value;
    if(category == 'Sensors'){
      this.addAssetForm.setControl("unit",new FormControl('',Validators.required));
    }
  }
  ngOnInit() {
  }
  closefailTips(){
    this.addfail=false;
  }
  cancel(){ 
    this.router.navigate(["/ui/nav/setting/asset-list"]);
  }
  save(){
    let asset:AssetModule = this.addAssetForm.value;
    if(asset.category == 'Sensors'){
      let realtimespec:AssetRealtimeDataSpecModule = new AssetRealtimeDataSpecModule();
      realtimespec.unit = this.addAssetForm.get('unit').value;
      realtimespec.validNumMax = this.addAssetForm.get('validNumMax').value;
      realtimespec.validNumMin = this.addAssetForm.get('validNumMin').value;
      asset.assetRealtimeDataSpec = realtimespec;
    }
    this.addassetloading = true;
    asset.assetSource = "flowgate";
    let assetStatus:AssetStatusModule = new AssetStatusModule();
    assetStatus.status = this.addAssetForm.get('status').value;
    asset.status = assetStatus
    this.service.createAnAsset(asset).subscribe(
      (data)=>{
        this.addassetloading = false;
        this.router.navigate(["/ui/nav/setting/asset-list"]);
      },error=>{
        this.addassetloading = false;
        if(error.status == 500)
        {
          this.addfail =true;
          this.tip = "Internal Server Error.";
        }
      }
    )
  }
}