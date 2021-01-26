/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { DcimService } from '../dcim.service';
import { Router } from '@angular/router';
import { FacilityModule } from '../../../facility.module';
import { FacilityAdapterModule } from 'app/pages/setting/component/adaptertype/facility-adapter.module';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
@Component({
  selector: 'app-dcim-add',
  templateUrl: './dcim-add.component.html',
  styleUrls: ['./dcim-add.component.scss']
})
export class DcimAddComponent implements OnInit {

  addDCIMForm:FormGroup;
  constructor(private service:DcimService,private router:Router,private fb: FormBuilder) { 
    this.addDCIMForm = this.fb.group({
      type: ['', [
        Validators.required
      ]],
      serverURL: ['', [
        Validators.required
      ]],
      name: ['', [
        Validators.required
      ]],
      description: ['', [
      ]],
      userName: ['', [
        Validators.required
      ]],
      password: ['', [
        Validators.required
      ]],
      verifyCert: ['true', [
        Validators.required
      ]]
    });
  }
 
  loading:boolean = false;
  dcimType:string = "";
  operatingModals:boolean = false;
  ignoreCertificatesModals:boolean = false;
  tip:string = "";

  dcimConfig:FacilityModule = new FacilityModule();

  read = "";/** This property is to change the read-only attribute of the password input box*/
  advanceSetting:string = "";
  seclectAdapter:FacilityAdapterModule = new FacilityAdapterModule();
  predefinedType:string[] = ['Nlyte','PowerIQ','OpenManage'];
  changetype(){
    
    let adapter:FacilityAdapterModule = this.adapterMap.get(this.addDCIMForm.get('type').value);
    this.dcimConfig.type = adapter.type;
    if(this.dcimConfig.type == "Nlyte"){
      this.dcimConfig.advanceSetting.PDU_POWER_UNIT = "KW";
    }else if(this.dcimConfig.type == "PowerIQ"){
      this.dcimConfig.advanceSetting.PDU_POWER_UNIT = "W";
    }else{
      this.dcimConfig.advanceSetting.PDU_POWER_UNIT = "W";
    }
  }

  save(){
      // this.read = "readonly";
      this.loading = true;
      let advanceSetting:any = this.dcimConfig.advanceSetting;
      this.dcimConfig =this.addDCIMForm.value;
      let adapter:FacilityAdapterModule = this.adapterMap.get(this.addDCIMForm.get('type').value);
      this.dcimConfig.type = adapter.type;
      if(this.predefinedType.indexOf(adapter.type) == -1){
        this.dcimConfig.subCategory = adapter.subCategory;
      }
      this.dcimConfig.advanceSetting = advanceSetting;
      this.service.AddDcimConfig(this.dcimConfig).subscribe(
        (data)=>{
          this.loading = false;
          this.router.navigate(["/ui/nav/facility/dcim/dcim-list"]);
        },(error:HttpErrorResponse)=>{
          if(error.status == 400 && error.error.errors[0] == "Invalid SSL Certificate"){
            this.loading = false;
            this.ignoreCertificatesModals = true;
            this.tip = error.error.message+". Are you sure you ignore the certificate check?"
          }else if(error.status == 400){
            this.loading = false;
            this.operatingModals = true;
            this.tip = error.error.message;
          }else{
            this.loading = false;
            this.operatingModals = true;
            this.tip = error.error.message+". Please check your input. ";
          }
        }
      )
  }
  confirmNoVerifyCertModal(){
    this.ignoreCertificatesModals = false;
    this.read = "";
    this.addDCIMForm.get('verifyCert').setValue('false');
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

  dcimAdapters:FacilityAdapterModule[] = [];
  adapterMap:Map<String,FacilityAdapterModule> = new Map<String,FacilityAdapterModule>();
  findAllAdapters(){
    this.service.findAllFacilityAdapters().subscribe(
      (data:FacilityAdapterModule[])=>{
        data.forEach(element => {
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
        let openManage:FacilityAdapterModule = new FacilityAdapterModule();
        openManage.displayName = "OpenManage";
        openManage.subCategory = "OpenManage";
        openManage.type = "OpenManage";
        this.dcimAdapters.push(openManage);
        this.dcimAdapters.forEach(element => {
          this.adapterMap.set(element.displayName,element);
        });
      }
    )
  }
  ngOnInit() {
    this.findAllAdapters();
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

}
