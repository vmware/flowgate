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

  constructor(private service:SettingService,private router:Router,private activedRoute:ActivatedRoute) { }
  ngOnInit() {
  }
  closefailTips(){
    this.addfail=false;
  }
  cancel(){ this.router.navigate(["/ui/nav/setting/asset-list"]);}
  save(){
    this.addassetloading = true;
    this.asset.assetSource = "flowgate";
    this.service.createAnAsset(this.asset).subscribe(
      (data)=>{
        if(data.status == 201){
          this.addassetloading = false;
          this.router.navigate(["/ui/nav/setting/asset-list"]);
        }
      },
      error=>{
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