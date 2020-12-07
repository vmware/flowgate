/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { VmwareService } from '../../vmware/vmware.service';
import { SddcsoftwareModule } from '../../../sddcsoftware.module';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
@Component({
  selector: 'app-vmware-config-add',
  templateUrl: './vmware-config-add.component.html',
  styleUrls: ['./vmware-config-add.component.scss']
})
export class VmwareConfigAddComponent implements OnInit {
  addSDDCForm:FormGroup;
  constructor(private service:VmwareService,private router:Router,private fb: FormBuilder) { 
    this.addSDDCForm = this.fb.group({
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
  verify:boolean = false;
  vmwareConfig:SddcsoftwareModule = new SddcsoftwareModule();

  read = "";/** This property is to change the read-only attribute of the password input box*/

  
  save(){
      this.loading = true;
      this.vmwareConfig = this.addSDDCForm.value;
      this.service.AddVmwareConfig(this.vmwareConfig).subscribe(
        (data)=>{
          this.loading = false;
          this.router.navigate(["/ui/nav/sddc/vmware/vmware-list"]);
        },(error:HttpErrorResponse)=>{

          if(error.status == 400 && error.error.message == "Certificate verification error"){
            this.loading = false;
            this.verify = true;
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
  Yes(){
    this.ignoreCertificatesModals = false;
    let verifyCert:boolean = this.addSDDCForm.get('verifyCert').value;
    if(verifyCert){
      this.addSDDCForm.get('verifyCert').setValue('false');
      this.save();
    }
  }
  No(){
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
  }


}
