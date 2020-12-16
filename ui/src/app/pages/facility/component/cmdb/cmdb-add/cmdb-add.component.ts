/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DcimService } from '../../dcim/dcim.service';
import { FormGroup, FormControl, Validators, FormBuilder } from "@angular/forms";
import { FacilityModule } from '../../../facility.module';
import { FacilityAdapterModule } from 'app/pages/setting/component/adaptertype/facility-adapter.module';
import { HttpErrorResponse } from '@angular/common/http';
@Component({
  selector: 'app-cmdb-add',
  templateUrl: './cmdb-add.component.html',
  styleUrls: ['./cmdb-add.component.scss']
})
export class CmdbAddComponent implements OnInit {
  addCMDBForm:FormGroup;
  constructor(private service:DcimService,private router:Router,private fb: FormBuilder) { 
    this.addCMDBForm = this.fb.group({
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
  operatingModals:boolean = false;
  ignoreCertificatesModals:boolean = false;
  tip:string = "";
  yes:boolean = false;
  no:boolean = true;

  cmdbConfig:FacilityModule = new FacilityModule();
  read = "";/** This property is to change the read-only attribute of the password input box*/
  enableProxySearch:boolean = false;
  isInfoblox:boolean = false;
  advanceSetting = {
    "INFOBLOX_PROXY_SEARCH":"LOCAL"
  }
  checkIsLabsDB(){
    this.addCMDBForm.setControl("userName",new FormControl('',Validators.required));
    this.addCMDBForm.setControl("password",new FormControl('',Validators.required));
    this.isInfoblox = false;
    let adapter:FacilityAdapterModule = this.adapterMap.get(this.addCMDBForm.get('type').value);
    this.cmdbConfig.type = adapter.type;
    if(this.cmdbConfig.type == "Labsdb"){
      this.addCMDBForm.setControl("userName",new FormControl('',Validators.nullValidator));
      this.addCMDBForm.setControl("password",new FormControl('',Validators.nullValidator));
    }else if(this.cmdbConfig.type == "InfoBlox"){
      this.isInfoblox = true;
    }
  }
  predefinedType:string[] = ['InfoBlox','Labsdb'];
  save(){
      this.read = "readonly";
      this.loading = true;
      this.cmdbConfig = this.addCMDBForm.value;
      if(this.isInfoblox && this.enableProxySearch){
        this.cmdbConfig.advanceSetting = this.advanceSetting;
      }
      let adapter:FacilityAdapterModule = this.adapterMap.get(this.addCMDBForm.get('type').value);
      this.cmdbConfig.type = adapter.type;
      if(this.predefinedType.indexOf(adapter.type) == -1){
        this.cmdbConfig.subCategory = adapter.subCategory;
      }
      this.service.AddDcimConfig(this.cmdbConfig).subscribe(
        (data)=>{
          this.loading = false;
          this.router.navigate(["/ui/nav/facility/cmdb/cmdb-list"]);
        },(error:HttpErrorResponse)=>{
          this.loading = false;
          if(error.status == 400 && error.error.errors[0] == "Invalid SSL Certificate"){
            this.ignoreCertificatesModals = true;
            this.tip = error.error.message+". Are you sure you ignore the certificate check?"
          }else if(error.status == 400){
            this.loading = false;
            this.operatingModals = true;
            this.tip = error.error.message;
          }else{
            this.operatingModals = true;
            this.tip = error.error.message+". Please check your input. ";
          }
        }
      )
  
  }
  Yes(){
    this.ignoreCertificatesModals = false;
    this.read = "";
    let verifyCert:boolean = this.addCMDBForm.get('verifyCert').value;
    if(verifyCert){
      this.addCMDBForm.get('verifyCert').setValue('false');
      this.save();
    }
  }
  No(){
    this.read = "";
    this.ignoreCertificatesModals = false;
    this.operatingModals = false;
  }
  cancel(){
    this.router.navigate(["/ui/nav/facility/cmdb/cmdb-list"]);
  }
  back(){
    this.router.navigate(["/ui/nav/facility/cmdb/cmdb-list"]);
  }
  seclectAdapter:FacilityAdapterModule = new FacilityAdapterModule();
  cmdbAdapters:FacilityAdapterModule[] = [];
  adapterMap:Map<String,FacilityAdapterModule> = new Map<String,FacilityAdapterModule>();
  findAllAdapters(){
    this.service.findAllFacilityAdapters().subscribe(
      (data:FacilityAdapterModule[])=>{
        data.forEach(element => {
          if(element.type == "OtherCMDB"){
            this.cmdbAdapters.push(element);
          }
        });
        let infoblox:FacilityAdapterModule = new FacilityAdapterModule();
        infoblox.displayName = "InfoBlox";
        infoblox.subCategory = "InfoBlox";
        infoblox.type = "InfoBlox";
        this.cmdbAdapters.push(infoblox);
        let labsdb:FacilityAdapterModule = new FacilityAdapterModule();
        labsdb.displayName = "Labsdb";
        labsdb.subCategory = "Labsdb";
        labsdb.type = "Labsdb";
        this.cmdbAdapters.push(labsdb);
        this.cmdbAdapters.forEach(element => {
          this.adapterMap.set(element.displayName,element);
        });
      }
    )
  }
  ngOnInit() {
    this.findAllAdapters();
  }

}
