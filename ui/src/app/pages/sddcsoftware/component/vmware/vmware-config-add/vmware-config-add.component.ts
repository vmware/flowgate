/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { error } from 'util';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import {Router,ActivatedRoute} from '@angular/router';
import { VmwareService } from '../../vmware/vmware.service';
import { SddcsoftwareModule } from '../../../sddcsoftware.module';
@Component({
  selector: 'app-vmware-config-add',
  templateUrl: './vmware-config-add.component.html',
  styleUrls: ['./vmware-config-add.component.scss']
})
export class VmwareConfigAddComponent implements OnInit {

  constructor(private service:VmwareService,private router:Router,private activedRoute:ActivatedRoute) { }

 
  loading:boolean = false;
  operatingModals:boolean = false;
  ignoreCertificatesModals:boolean = false;
  tip:string = "";
  verify:boolean = false;
  vmwareConfig:SddcsoftwareModule = new SddcsoftwareModule();

  read = "";/** This property is to change the read-only attribute of the password input box*/

  
  save(){
      this.read = "readonly";
      this.loading = true;
      this.service.AddVmwareConfig(this.vmwareConfig).subscribe(
        (data)=>{
          if(data.status == 201){
            this.loading = false;
            this.router.navigate(["/ui/nav/sddc/vmware/vmware-list"]);
          }
        },
        error=>{
          if(error.status == 400 && error.json().message == "Certificate verification error"){
            this.loading = false;
            this.verify = true;
            this.ignoreCertificatesModals = true;
            this.tip = error.json().message+". Are you sure you ignore the certificate check?"
          }else if(error.status == 400 && error.json().message == "UnknownHostException"){
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
      this.vmwareConfig.verifyCert = "false";
      this.save();
    }
  }
  No(){
    this.read = "";
    this.ignoreCertificatesModals = false;
    this.operatingModals = false;
  }
  cancel(){
    this.router.navigate(["/ui/nav/sddc/vmware/vmware-list"]);
  }
  back(){
    this.router.navigate(["/ui/nav/sddc/vmware/vmware-list"]);
  }
  ngOnInit() {
    this.vmwareConfig.verifyCert = "true";
  }

  

}
