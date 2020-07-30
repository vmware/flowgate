/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { AssetModule } from '../asset-modules/asset.module';
import {Router,ActivatedRoute} from '@angular/router';
import { SettingService } from '../../setting.service';
import { error } from 'util';
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
  save(){
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