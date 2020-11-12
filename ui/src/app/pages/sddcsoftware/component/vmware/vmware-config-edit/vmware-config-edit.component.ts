/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { Router,ActivatedRoute } from '@angular/router';
import { VmwareService } from '../../vmware/vmware.service';
import { SddcsoftwareModule } from '../../../sddcsoftware.module';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
@Component({
  selector: 'app-vmware-config-edit',
  templateUrl: './vmware-config-edit.component.html',
  styleUrls: ['./vmware-config-edit.component.scss']
})
export class VmwareConfigEditComponent implements OnInit {
  editSDDCForm:FormGroup;
  constructor(private service:VmwareService,private router:Router,private activedRoute:ActivatedRoute,private fb: FormBuilder) {
    this.editSDDCForm = this.fb.group({
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
      id: ['', [
      ]],
      userId: ['', [
      ]],
      integrationStatus: ['', [
      ]],
      subCategory: ['', [
      ]],
      userName: ['', [
        Validators.required
      ]],
      password: ['', [
        Validators.required
      ]],
      verifyCert: ['', [
        Validators.required
      ]]
    });
   }
  loading:boolean = false;
  operatingModals:boolean = false;
  ignoreCertificatesModals:boolean = false;
  tip:string = "";
  verify:boolean = false;
 
  checked:boolean;
  read = "";
  vmwareConfig:SddcsoftwareModule = new SddcsoftwareModule();
  save(){
    this.vmwareConfig = this.editSDDCForm.value;
      this.loading = true;
      this.service.updateVmwareConfig(this.vmwareConfig).subscribe(
        (data)=>{
            this.loading = false;
            this.router.navigate(["/ui/nav/sddc/vmware/vmware-list"]);
        },(error:HttpErrorResponse)=>{
          if(error.status == 400 && error.error.message == "Certificate verification error"){
            this.loading = false;
            this.verify = true;
            this.ignoreCertificatesModals = true;
            this.tip = error.error.message+". Are you sure you ignore the certificate check?"
          }else if(error.status == 400 && error.error.message == "UnknownHostException"){
            this.loading = false;
            this.operatingModals = true;
            this.tip = error.error.message+". Please check your serverIp. ";
          }else if(error.status == 401){
            this.loading = false;
            this.operatingModals = true;
            this.tip = error.error.message+". Please check your userName or password. ";
          }else{
            this.loading = false;
            this.operatingModals = true;
            this.tip = error.error.message+". Please check your input. ";
          }
        }
      )
  
  }
  Yes(){
    this.operatingModals = false;
    this.read = "";
    if(this.verify){
      this.vmwareConfig.verifyCert = false;
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
    this.vmwareConfig.id = this.activedRoute.snapshot.params['id'];
    if(this.vmwareConfig.id != null && this.vmwareConfig.id != ""){
      this.service.getVmwareConfig(this.vmwareConfig.id).subscribe(
        (data:SddcsoftwareModule)=>{
          this.editSDDCForm.setValue(data);
          if(this.vmwareConfig.verifyCert){
            this.editSDDCForm.get('verifyCert').setValue('true');
          }else{
            this.editSDDCForm.get('verifyCert').setValue('false');
          }
        }
      )
    }
  }

}
