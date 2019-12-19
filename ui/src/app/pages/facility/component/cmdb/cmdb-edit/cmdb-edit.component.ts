/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { error } from 'util';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import {Router,ActivatedRoute} from '@angular/router';
import { DcimService } from '../../dcim/dcim.service';
import {FormGroup, FormControl, Validators} from "@angular/forms";
import { FacilityModule } from '../../../facility.module';
@Component({
  selector: 'app-cmdb-edit',
  templateUrl: './cmdb-edit.component.html',
  styleUrls: ['./cmdb-edit.component.scss']
})
export class CmdbEditComponent implements OnInit {

  constructor(private service:DcimService,private router:Router,private activedRoute:ActivatedRoute) { }
  cmdbForm = new FormGroup({
    cmdbtype:new FormControl({value:'',disabled: true},Validators.required),
    serverIPInput:new FormControl({value:'',disabled: true},Validators.required),
    serverName:new FormControl('',Validators.required),
    userName:new FormControl('',Validators.required),
    passwordInput:new FormControl('',Validators.required)
  });
  userId:string="";
  loading:boolean = false;
  operatingModals:boolean = false;
  ignoreCertificatesModals:boolean = false;
  tip:string = "";
  verify:boolean = false;
  checked:boolean;
  yes:boolean = false;
  no:boolean = true;

  cmdbConfig:FacilityModule = new FacilityModule();
  read = "";/** This property is to change the read-only attribute of the password input box*/
  advanceSetting = {
    "INFOBLOX_PROXY_SEARCH":"LOCAL"
  }
  isInfoblox:boolean = false;
  enableProxySearch:boolean = false;
  checkIsLabsDB(){
    this.cmdbForm.setControl("userName",new FormControl('',Validators.required));
    this.cmdbForm.setControl("passwordInput",new FormControl('',Validators.required));
    if(this.cmdbConfig.type == "Labsdb"){
      this.cmdbForm.setControl("userName",new FormControl('',Validators.nullValidator));
      this.cmdbForm.setControl("passwordInput",new FormControl('',Validators.nullValidator));
    }else if(this.cmdbConfig.type == "InfoBlox"){
      this.isInfoblox = true;
      if(this.cmdbConfig.advanceSetting != null && this.cmdbConfig.advanceSetting.INFOBLOX_PROXY_SEARCH != ""){
        this.enableProxySearch = true;
        this.advanceSetting = this.cmdbConfig.advanceSetting;
      }
    }
  }

  save(){
      this.read = "readonly";
      this.loading = true;
      if(this.isInfoblox){
        if(this.enableProxySearch){
          this.cmdbConfig.advanceSetting = this.advanceSetting;
        }else{
          this.cmdbConfig.advanceSetting.INFOBLOX_PROXY_SEARCH = ""
        }
      }
      this.service.updateFacility(this.cmdbConfig).subscribe(
        (data)=>{
          if(data.status == 200){
            this.loading = false;
            this.router.navigate(["/ui/nav/facility/cmdb/cmdb-list"]);
          }
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
  ngOnInit() {
    this.cmdbConfig.id = this.activedRoute.snapshot.params['id'];

    if(this.cmdbConfig.id != null && this.cmdbConfig.id != ""){
      this.service.getDcimConfig(this.cmdbConfig.id).subscribe(
        (data)=>{
          if(data.status == 200){
            if(data.json != null){
              this.cmdbConfig = data.json();
              this.checked =  data.json().verifyCert;
              if(this.checked == false){
                this.cmdbConfig.verifyCert = "false";
              }else{
                this.cmdbConfig.verifyCert = "true";
              }
              this.checkIsLabsDB();
            }
          }
        }
      )
    }
  }

}
