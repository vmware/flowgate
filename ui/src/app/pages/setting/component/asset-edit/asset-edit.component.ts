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
      this.editAssetForm.controls["unit"].setValidators([Validators.required]);
      this.editAssetForm.controls["mountingSide"].setValidators([Validators.required]);
      this.editAssetForm.controls["subCategory"].setValidators([Validators.required]);
    }
    if(category == 'Server'){
      this.editAssetForm.controls["unit"].clearValidators();
      this.editAssetForm.controls["mountingSide"].setValidators([Validators.required]);
      this.editAssetForm.controls["subCategory"].setValidators([Validators.required]);
    }
    if(category == 'Cabinet'){
      this.editAssetForm.controls["unit"].clearValidators();
      this.editAssetForm.controls["mountingSide"].clearValidators();
      this.editAssetForm.controls["subCategory"].clearValidators();
    }
    if(category == 'PDU' || category == 'Networks' || category == 'UPS' || category == 'Chassis'){
      this.editAssetForm.controls["unit"].clearValidators();
      this.editAssetForm.controls["mountingSide"].setValidators([Validators.required]);
      this.editAssetForm.controls["subCategory"].clearValidators();
    }
    this.editAssetForm.controls['unit'].updateValueAndValidity();
    this.editAssetForm.controls['mountingSide'].updateValueAndValidity();
    this.editAssetForm.controls['subCategory'].updateValueAndValidity();
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
          if(assetTemp.assetRealtimeDataSpec != null){
            assetTemp.unit = assetTemp.assetRealtimeDataSpec.unit == null ? "": assetTemp.assetRealtimeDataSpec.unit;
            assetTemp.validNumMax = assetTemp.assetRealtimeDataSpec.validNumMax == null ? "": assetTemp.assetRealtimeDataSpec.validNumMax;
            assetTemp.validNumMin = assetTemp.assetRealtimeDataSpec.validNumMin == null ? "": assetTemp.assetRealtimeDataSpec.validNumMin;
          }
          this.editAssetForm.setValue(assetTemp);
          this.editAssetForm.get("status").setValue(assetTemp.status.status);
          this.changeType();
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
      else return value;
  }

  advanceCheckbox(){
    this.advance = !this.advance;
    this.baseconfig = !this.baseconfig;
    let asset:AssetModule = this.editAssetForm.value;
    if(asset.category == 'Sensors'){
      let realtimespec:AssetRealtimeDataSpecModule = new AssetRealtimeDataSpecModule();
      realtimespec.unit = this.editAssetForm.get('unit').value;
      realtimespec.validNumMax = this.editAssetForm.get('validNumMax').value;
      realtimespec.validNumMin = this.editAssetForm.get('validNumMin').value;
      asset.assetRealtimeDataSpec = realtimespec;
    }
    let assetStatus:AssetStatusModule = new AssetStatusModule();
    assetStatus.status = this.editAssetForm.get('status').value;
    asset.status = assetStatus
    this.assetjsondata = this.jsonFormater(JSON.stringify(asset, this.replacer));
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

      if(asset.category == 'Cabinet'){
        asset.cabinetAssetNumber = this.editAssetForm.get('cabinetAssetNumber').value;
        asset.capacity = this.editAssetForm.get('capacity').value;
      }else{
        asset.mountingSide = this.editAssetForm.get('mountingSide').value;
        asset.cabinetUnitPosition = this.editAssetForm.get('cabinetUnitPosition').value;
      }
      if(asset.category == 'Server'){
        asset.subCategory = this.editAssetForm.get('subCategory').value;
      }
      
      asset.cabinetName = this.editAssetForm.get('cabinetName').value;
      asset.assetName = this.editAssetForm.get('assetName').value;
      asset.assetNumber = this.editAssetForm.get('assetNumber').value;
      asset.manufacturer = this.editAssetForm.get('manufacturer').value;
      asset.serialnumber = this.editAssetForm.get('serialnumber').value;
      asset.model = this.editAssetForm.get('model').value;
      asset.tag = this.editAssetForm.get('tag').value;
      asset.region = this.editAssetForm.get('region').value;
      asset.country = this.editAssetForm.get('country').value;
      asset.city = this.editAssetForm.get('city').value;
      asset.building = this.editAssetForm.get('building').value;
      asset.floor = this.editAssetForm.get('floor').value;
      asset.room = this.editAssetForm.get('room').value;
      asset.row = this.editAssetForm.get('row').value;
      asset.col = this.editAssetForm.get('col').value;

      this.editassetloading = true;
      asset.assetSource = "flowgate";
      let assetStatus:AssetStatusModule = new AssetStatusModule();
      assetStatus.status = this.editAssetForm.get('status').value;
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