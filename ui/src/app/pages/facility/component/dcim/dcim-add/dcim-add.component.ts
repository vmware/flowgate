/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { DcimService } from '../dcim.service';
import { error } from 'util';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import {Router,ActivatedRoute} from '@angular/router';
import { FacilityModule } from '../../../facility.module';
@Component({
  selector: 'app-dcim-add',
  templateUrl: './dcim-add.component.html',
  styleUrls: ['./dcim-add.component.scss']
})
export class DcimAddComponent implements OnInit {

  constructor(private service:DcimService,private router:Router,private activedRoute:ActivatedRoute) { }

 
  loading:boolean = false;
  dcimType:string = "";
  operatingModals:boolean = false;
  ignoreCertificatesModals:boolean = false;
  tip:string = "";
  nlyteAdvanceSettingShow:boolean = false;
  powerIQAdvanceSettingShow:boolean = false;
  commonAdvanceSettingShow:boolean = true;
  dcimConfig:FacilityModule = new FacilityModule();

  read = "";/** This property is to change the read-only attribute of the password input box*/
  advanceSetting:string = "";
 
 
  changetype(){

    if(this.dcimConfig.type == "Nlyte"){
      this.nlyteAdvanceSettingShow = true;
      this.powerIQAdvanceSettingShow = false;
      this.dcimConfig.advanceSetting.PDU_POWER_UNIT = "KW";
    }else if(this.dcimConfig.type == "PowerIQ"){
      this.powerIQAdvanceSettingShow = true;
      this.nlyteAdvanceSettingShow = false;
      this.dcimConfig.advanceSetting.PDU_POWER_UNIT = "W";
    }else{
      this.powerIQAdvanceSettingShow = false;
      this.nlyteAdvanceSettingShow = false;
      this.dcimConfig.advanceSetting.PDU_POWER_UNIT = "W";
    }
  }

  save(){
      this.read = "readonly";
      this.loading = true;
      this.service.AddDcimConfig(this.dcimConfig).subscribe(
        (data)=>{
          if(data.status == 201){
            this.loading = false;
            this.router.navigate(["/ui/nav/facility/dcim/dcim-list"]);
          }
        },
        error=>{
          if(error.status == 400 && error.json().errors[0] == "Invalid SSL Certificate"){
            this.loading = false;
            this.ignoreCertificatesModals = true;
            this.tip = error.json().message+". Are you sure you ignore the certificate check?"
          }else if(error.status == 400 && error.json().errors[0] == "Unknown Host"){
            this.loading = false;
            this.operatingModals = true;
            this.tip = error.json().message+". Please check your serverIp. ";
          }else if(error.status == 401){
            this.loading = false;
            this.operatingModals = true;
            this.tip = error.json().message+". Please check your userName or password. ";
          }else{
            this.loading = false;
            this.operatingModals = true;
            this.tip = error.json().message+". Please check your input. ";
          }
        }
      )
  
  }
  confirmNoVerifyCertModal(){
    this.ignoreCertificatesModals = false;
    this.read = "";
    this.dcimConfig.verifyCert = "false";
    this.save();
  }
  closeVerifyCertModal(){
    this.read = "";
    this.ignoreCertificatesModals = false;
  }
  closeOperationTips(){
    this.read = "";
    this.operatingModals = false;
  }
  cancel(){
    this.router.navigate(["/ui/nav/facility/dcim/dcim-list"]);
  }
  back(){
    this.router.navigate(["/ui/nav/facility/dcim/dcim-list"]);
  }
  ngOnInit() {
    this.dcimConfig.advanceSetting = {
      DateFormat:"",
      TimeZone:"",
      PDU_POWER_UNIT:"KW",
      PDU_AMPS_UNIT:"A",
      PDU_VOLT_UNIT:"V",
      TEMPERATURE_UNIT:"C",
      HUMIDITY_UNIT:"%"
    }
    this.dcimConfig.verifyCert = "true";
  }

}
