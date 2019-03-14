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
@Component({
  selector: 'app-cmdb-edit',
  templateUrl: './cmdb-edit.component.html',
  styleUrls: ['./cmdb-edit.component.scss']
})
export class CmdbEditComponent implements OnInit {

  constructor(private service:DcimService,private router:Router,private activedRoute:ActivatedRoute) { }

  userId:string="";
  loading:boolean = false;
  operatingModals:boolean = false;
  ignoreCertificatesModals:boolean = false;
  tip:string = "";
  verify:boolean = false;
  checked:boolean;
  yes:boolean = false;
  no:boolean = true;
  dcimConfig={
    id:"",
    type:"",
    name:"",
    description:"",
    serverURL:"",
    userName:"",
    password:"",
    verifyCert:"true"
  }
  read = "";/** This property is to change the read-only attribute of the password input box*/
  
  
  save(){
      this.read = "readonly";
      this.loading = true;
      this.service.updateDcimConfig(this.dcimConfig.id,this.dcimConfig.type,this.dcimConfig.name,this.dcimConfig.description,this.dcimConfig.userName,
        this.dcimConfig.password,this.dcimConfig.serverURL,this.dcimConfig.verifyCert,null).subscribe(
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
      this.dcimConfig.verifyCert = "false";
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
    this.dcimConfig.id = window.sessionStorage.getItem("editdcimconfigid");

    if(this.dcimConfig.id != null && this.dcimConfig.id != ""){
      this.service.getDcimConfig(this.dcimConfig.id).subscribe(
        (data)=>{
          if(data.status == 200){
            if(data.json != null){
              this.dcimConfig = data.json();
              this.checked =  data.json().verifyCert;
              if(this.checked == false){
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
