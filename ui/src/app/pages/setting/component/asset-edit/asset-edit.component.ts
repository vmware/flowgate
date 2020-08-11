/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { AssetModule } from '../asset-modules/asset.module';
import {Router,ActivatedRoute} from '@angular/router';
import { SettingService } from '../../setting.service';
import { error } from 'util';
import { stringify } from 'querystring';
@Component({
  selector: 'app-asset-edit',
  templateUrl: './asset-edit.component.html',
  styleUrls: ['./asset-edit.component.scss']
})
export class AssetEditComponent implements OnInit {
  description:any;
  asset:AssetModule = new AssetModule();
  addfail:boolean =false;
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

  constructor(private service:SettingService,private router:Router,private activedRoute:ActivatedRoute) { }
  ngOnInit() {
    this.asset.id = this.activedRoute.snapshot.params['id'];
    if(this.asset.id != null && this.asset.id != ""){
      this.service.getAssetsByID(this.asset.id).subscribe(
        (data)=>{
          if(data.status == 200){
            if(data.json != null){
              this.asset = data.json();
            }
          }
        }
      )
    }
  }
  closefailTips(){
    this.addfail=false;
  }
  cancel(){ this.router.navigate(["/ui/nav/setting/asset-list"]);}
  jsonFormater(json:any) {
    var obj = JSON.parse(json);
    var formated = JSON.stringify(obj, undefined, 4);
    return formated;
  }
  advanceCheckbox(){
    this.advance = !this.advance;
    this.baseconfig = !this.baseconfig;
    this.assetjsondata = this.jsonFormater(JSON.stringify(this.asset));
  }
  checkJsonFormat(){
    try {
      JSON.parse(this.assetjsondata);
      this.checkjsonformat = false;
      this.jsonerror = false;
    } catch(e) {
        this.jsonerrormsg = String(e);
        this.jsonerror = true;
        this.checkjsonformat = true;
    }
  }
  save(){

    if(this.advance == true){
      let assetobj = JSON.parse(this.assetjsondata);
      if(assetobj.id != this.asset.id){
        this.addfail = true;
        this.tip = "Don't change asset ID.";
        return;
      }
      this.asset = JSON.parse(this.assetjsondata);
    }
    
    this.editassetloading = true;
    this.service.updateAssetsByID(this.asset).subscribe(
      (data)=>{
        if(data.status == 200){
          this.editassetloading = false;
          this.router.navigate(["/ui/nav/setting/asset-list"]);
        }
      },
      error=>{
        this.editassetloading = false;
        if(error.status == 400)
        {
          this.addfail =true;
          this.tip = "Bad Request.";
        }
        if(error.status == 500)
        {
          this.addfail =true;
          this.tip = "Internal Server Error.";
        }
      }
    )
  }
}