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
import { FacilityAdapterModule } from 'app/pages/setting/component/adaptertype/facility-adapter.module';
@Component({
  selector: 'app-dcim-edit',
  templateUrl: './dcim-edit.component.html',
  styleUrls: ['./dcim-edit.component.scss']
})
export class DcimEditComponent implements OnInit {

  constructor(private service:DcimService,private router:Router,private activedRoute:ActivatedRoute) { }

  userId:string="";
  loading:boolean = false;
  operatingModals:boolean = false;
  ignoreCertificatesModals:boolean = false;
  tip:string = "";
  nlyteAdvanceSettingShow:boolean = false;
  powerIQAdvanceSettingShow:boolean = false;
  commonAdvanceSettingShow:boolean = true;
  dcimConfig:FacilityModule = new FacilityModule();
  
  read = "";/** This property is to change the read-only attribute of the password input box*/


  changetype(){
    this.dcimConfig.type = this.adapterMap.get(this.seclectAdapter.subCategory).type;
    this.dcimConfig.subCategory = this.seclectAdapter.subCategory;
    if(this.dcimConfig.type == "Nlyte"){
      this.nlyteAdvanceSettingShow = true;
      this.powerIQAdvanceSettingShow = false;
    }else if(this.dcimConfig.type == "PowerIQ"){
      this.powerIQAdvanceSettingShow = true;
      this.nlyteAdvanceSettingShow = false;
    }else{
      this.powerIQAdvanceSettingShow = false;
      this.nlyteAdvanceSettingShow = false;
    }
  }
  
  save(){
      this.read = "readonly";
      this.loading = true;
      this.service.updateFacility(this.dcimConfig).subscribe(
        (data)=>{
          if(data.status == 200){
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
  seclectAdapter:FacilityAdapterModule = new FacilityAdapterModule();
  dcimAdapters:FacilityAdapterModule[] = [];
  adapterMap:Map<String,FacilityAdapterModule> = new Map<String,FacilityAdapterModule>();
  findAllAdapters(){
    this.service.findAllFacilityAdapters().subscribe(
      (data)=>{
        let allFacilityAdapters:FacilityAdapterModule[] = [];
        allFacilityAdapters = data.json();
        allFacilityAdapters.forEach(element => {
          if(element.type == "OtherDCIM"){
            this.dcimAdapters.push(element);
          }
        });
        let nlyte:FacilityAdapterModule = new FacilityAdapterModule();
        nlyte.displayName = "Nlyte";
        nlyte.subCategory = "Nlyte";
        nlyte.type = "Nlyte";
        this.dcimAdapters.push(nlyte);
        let powerIQ:FacilityAdapterModule = new FacilityAdapterModule();
        powerIQ.displayName = "PowerIQ";
        powerIQ.subCategory = "PowerIQ";
        powerIQ.type = "PowerIQ";
        this.dcimAdapters.push(powerIQ);
        this.dcimAdapters.forEach(element => {
          this.adapterMap.set(element.subCategory,element);
        });
      }
    )
  }

  ngOnInit() {
    this.findAllAdapters();
    this.dcimConfig.advanceSetting ={
      DateFormat:"",
      TimeZone:"",
      PDU_POWER_UNIT:"",
      PDU_AMPS_UNIT:"",
      PDU_VOLT_UNIT:"",
      TEMPERATURE_UNIT:"",
      HUMIDITY_UNIT:""
    }
    this.dcimConfig.id = this.activedRoute.snapshot.params['id'];
   
    if(this.dcimConfig.id != null && this.dcimConfig.id != ""){
      this.service.getDcimConfig(this.dcimConfig.id).subscribe(
        (data)=>{
          if(data.status == 200){
            if(data.json != null){
              this.dcimConfig = data.json();
              this.seclectAdapter.subCategory = this.dcimConfig.subCategory;
              if(data.json().advanceSetting == null){
                this.dcimConfig.advanceSetting = {
                  DateFormat:"",
                  TimeZone:"",
                  PDU_POWER_UNIT:"KW",
                  PDU_AMPS_UNIT:"A",
                  PDU_VOLT_UNIT:"V",
                  TEMPERATURE_UNIT:"C",
                  HUMIDITY_UNIT:"%"
                }
              }
              if(this.dcimConfig.verifyCert == false){
                this.dcimConfig.verifyCert = "false";
              }else{
                this.dcimConfig.verifyCert = "true";
              }
            }
          }
        }
      )
    }
  }

}
