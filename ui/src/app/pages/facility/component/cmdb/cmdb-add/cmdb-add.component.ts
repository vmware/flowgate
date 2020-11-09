/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DcimService } from '../../dcim/dcim.service';
import { FormGroup, FormControl, Validators } from "@angular/forms";
import { FacilityModule } from '../../../facility.module';
import { FacilityAdapterModule } from 'app/pages/setting/component/adaptertype/facility-adapter.module';
@Component({
  selector: 'app-cmdb-add',
  templateUrl: './cmdb-add.component.html',
  styleUrls: ['./cmdb-add.component.scss']
})
export class CmdbAddComponent implements OnInit {

  constructor(private service:DcimService,private router:Router) { }


  cmdbForm = new FormGroup({
    serverIPInput:new FormControl('',Validators.required),
    serverName:new FormControl('',Validators.required),
    userName:new FormControl('',Validators.required),
    passwordInput:new FormControl('',Validators.required)
  });

  loading:boolean = false;
  operatingModals:boolean = false;
  ignoreCertificatesModals:boolean = false;
  tip:string = "";
  verify:boolean = false;
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
    this.cmdbForm.setControl("userName",new FormControl('',Validators.required));
    this.cmdbForm.setControl("passwordInput",new FormControl('',Validators.required));
    let adapter:FacilityAdapterModule = this.adapterMap.get(this.seclectAdapter.displayName);
    this.cmdbConfig.type = adapter.type;
    if(this.cmdbConfig.type != adapter.displayName){
      this.cmdbConfig.subCategory = adapter.subCategory;
    }
    if(this.cmdbConfig.type == "Labsdb"){
      this.cmdbForm.setControl("userName",new FormControl('',Validators.nullValidator));
      this.cmdbForm.setControl("passwordInput",new FormControl('',Validators.nullValidator));
    }else {
      this.isInfoblox = true;
    }
  }

  save(){
      this.read = "readonly";
      this.loading = true;
      if(this.isInfoblox && this.enableProxySearch){
        this.cmdbConfig.advanceSetting = this.advanceSetting;
      }
      this.service.AddDcimConfig(this.cmdbConfig).subscribe(
        (data)=>{
          this.loading = false;
          this.router.navigate(["/ui/nav/facility/cmdb/cmdb-list"]);
        },
        error=>{
          if(error.status == 400 && error.json().errors[0] == "Invalid SSL Certificate"){
            this.loading = false;
            this.verify = true;
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
  Yes(){
    this.ignoreCertificatesModals = false;
    this.read = "";
    if(this.verify){
      this.cmdbConfig.verifyCert = "false";
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
    this.cmdbConfig.verifyCert = "true";
  }

}
