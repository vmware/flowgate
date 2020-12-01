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
import { AssetTempModule } from '../asset-modules/asset-temp.module';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-asset-edit',
  templateUrl: './asset-edit.component.html',
  styleUrls: ['./asset-edit.component.scss']
})
export class AssetEditComponent implements OnInit {
  description:any;
  asset:AssetModule = new AssetModule();
  editfail:boolean =false;
  editassetloading:boolean = false;
  tip:string = " ";
  allcategory=[ "Server", "PDU","Cabinet","Networks","Sensors","UPS","Chassis"];
  suballcategory = {"Server":["Blade","Standard"], 
  "Sensors":["Humidity","Temperature","AirPressure","AirFlow","ContactClosure","Smoke","Water","Vibration"]};
  assetjsondata:string = " ";
  baseconfig:boolean = true;
  advance:boolean = false;
  jsonerror:boolean = false;
  checkjsonformat:boolean = false;
  jsonerrormsg:string = " ";
  editAssetForm:FormGroup;
  constructor(private service:SettingService,private router:Router,private fb: FormBuilder, private activedRoute:ActivatedRoute) { 
    this.editAssetForm = this.fb.group({
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
      status:  ['', [
      ]],
      statusDisplay: ['', [
        Validators.required
      ]],
      assetRealtimeDataSpec: ['', [
      ]],
      id: ['', [
      ]],
      assetSource: ['', [
      ]],
      assetAddress: ['', [
      ]],
      extraLocation: ['', [
      ]],
      freeCapacity: ['', [
      ]],
      justificationfields: ['', [
      ]],
      parent: ['', [
      ]],
      pdus: ['', [
      ]],
      switches: ['', [
      ]],
      metricsformulars: ['', [
      ]],
      lastupdate: ['', [
      ]],
      created: ['', [
      ]],
      tenant: ['', [
      ]]
    });
  }
  changeType(){
    let category:string = this.editAssetForm.get('category').value;
    if(category == 'Sensors'){
      this.editAssetForm.setControl("unit",new FormControl('',Validators.required));
    }
  }
  ngOnInit() {
    this.asset.id = this.activedRoute.snapshot.params['id'];
    if(this.asset.id != null && this.asset.id != ""){
      this.service.getAssetsByID(this.asset.id).subscribe(
        (assetTemp:AssetTempModule)=>{
          this.asset = assetTemp;
          assetTemp.unit = "";
          assetTemp.validNumMax = "";
          assetTemp.validNumMin = "";
          assetTemp.statusDisplay = "";
          if(assetTemp.assetRealtimeDataSpec != null){
            assetTemp.unit = assetTemp.assetRealtimeDataSpec.unit == null ? "": assetTemp.assetRealtimeDataSpec.unit;
            assetTemp.validNumMax = assetTemp.assetRealtimeDataSpec.validNumMax == null ? "": assetTemp.assetRealtimeDataSpec.validNumMax;
            assetTemp.validNumMin = assetTemp.assetRealtimeDataSpec.validNumMin == null ? "": assetTemp.assetRealtimeDataSpec.validNumMin;
          }
          if(assetTemp.status != null){
            assetTemp.statusDisplay = assetTemp.status.status == null ? "": assetTemp.status.status;
          }
          this.assetjsondata = this.jsonFormater(JSON.stringify(this.asset, this.replacer));
          this.editAssetForm.setValue(assetTemp);
        }
      )
    }
  }
  closefailTips(){
    this.editfail=false;
  }
  cancel(){ 
    this.router.navigate(["/ui/nav/setting/asset-list"]);
  }
  jsonFormater(json:any) {
    var obj = JSON.parse(json);
    var formated = JSON.stringify(obj, undefined, 4);
    return formated;
  }

  replacer(key, value)
  {
      if (key=="unit") return undefined;
      else if (key=="validNumMin") return undefined;
      else if (key=="validNumMax") return undefined;
      else if (key=="statusDisplay") return undefined;
      else return value;
  }

  advanceCheckbox(){
    this.advance = !this.advance;
    this.baseconfig = !this.baseconfig;
  }

  checkJsonFormat(){
    let jsondata = this.assetjsondata;
    try {
      JSON.parse(jsondata);
      this.checkjsonformat = false;
      this.jsonerror = false;
    } catch(e) {
        this.jsonerrormsg = String(e);
        this.jsonerror = true;
        this.checkjsonformat = true;
    }
  }
  save(){

    let asset:AssetModule = new AssetModule();
    if(this.advance == true){
      let jsondata = this.assetjsondata;
      asset = JSON.parse(jsondata);

      if(asset.id != this.asset.id){
        this.editfail = true;
        this.tip = "Don't change asset ID.";
        return;
      }
    }else{
      asset= this.editAssetForm.value;
      if(asset.category == 'Sensors'){
        let realtimespec:AssetRealtimeDataSpecModule = new AssetRealtimeDataSpecModule();
        realtimespec.unit = this.editAssetForm.get('unit').value;
        realtimespec.validNumMax = this.editAssetForm.get('validNumMax').value;
        realtimespec.validNumMin = this.editAssetForm.get('validNumMin').value;
        asset.assetRealtimeDataSpec = realtimespec;
      }
      this.editassetloading = true;
      asset.assetSource = "flowgate";
      let assetStatus:AssetStatusModule = new AssetStatusModule();
      assetStatus.status = this.editAssetForm.get('statusDisplay').value;
      asset.status = assetStatus
    }

    this.service.updateAssetsByID(asset).subscribe(
      (data)=>{
        this.editassetloading = false;
        this.router.navigate(["/ui/nav/setting/asset-list"]);
      },(error:HttpErrorResponse)=>{
        this.editassetloading = false;
        if(error.status == 500)
        {
          this.editfail =true;
          this.tip = "Internal Server Error.";
        }
      }
    )
  }
}