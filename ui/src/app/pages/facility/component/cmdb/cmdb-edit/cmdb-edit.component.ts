/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import {Router,ActivatedRoute} from '@angular/router';
import { DcimService } from '../../dcim/dcim.service';
import {FormGroup, Validators, FormBuilder, FormControl} from "@angular/forms";
import { FacilityModule } from '../../../facility.module';
import { FacilityAdapterModule } from 'app/pages/setting/component/adaptertype/facility-adapter.module';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, SubscribableOrPromise } from 'rxjs';
import { map } from 'rxjs/operators';
@Component({
  selector: 'app-cmdb-edit',
  templateUrl: './cmdb-edit.component.html',
  styleUrls: ['./cmdb-edit.component.scss']
})
export class CmdbEditComponent implements OnInit {

  editCMDBForm:FormGroup;
  constructor(private service:DcimService,private router:Router,private activedRoute:ActivatedRoute,private fb: FormBuilder) {
    this.editCMDBForm = this.fb.group({
      type: [{value:'',disabled: true}, [
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
      advanceSetting: ['', [
      ]],
      subCategory: ['', [
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
    this.editCMDBForm.controls["userName"].setValidators([Validators.required]);
    this.editCMDBForm.controls["password"].setValidators([Validators.required]);
    this.isInfoblox = false;
    let subcategory:string =  this.editCMDBForm.get('type').value;
    let adapter:FacilityAdapterModule = this.adapterMap.get(subcategory);
    if(adapter.type == "Labsdb"){
      this.editCMDBForm.controls["userName"].clearValidators();
      this.editCMDBForm.controls["userName"].setValidators([Validators.nullValidator]);
      this.editCMDBForm.controls["password"].clearValidators();
      this.editCMDBForm.controls["password"].setValidators([Validators.nullValidator]);
    }else if(adapter.type == "InfoBlox"){
      this.isInfoblox = true;
      if(this.editCMDBForm.get('advanceSetting').value != null && this.editCMDBForm.get('advanceSetting').value.INFOBLOX_PROXY_SEARCH != ""){
        this.enableProxySearch = true;
        this.advanceSetting = this.editCMDBForm.get('advanceSetting').value;
      }
    }
    this.editCMDBForm.controls['userName'].updateValueAndValidity();
    this.editCMDBForm.controls['password'].updateValueAndValidity();
  }

  save(){
      this.read = "readonly";
      this.loading = true;
      this.cmdbConfig = this.editCMDBForm.value;
      if(this.isInfoblox && this.editCMDBForm.get('advanceSetting').value != null){
        if(this.enableProxySearch){
          this.editCMDBForm.get('advanceSetting').setValue(this.advanceSetting);
        }else{
          this.advanceSetting.INFOBLOX_PROXY_SEARCH = "";
          this.editCMDBForm.get('advanceSetting').setValue(this.advanceSetting);
        }
      }
      let adapter:FacilityAdapterModule = this.adapterMap.get(this.editCMDBForm.get('type').value);
      this.cmdbConfig.type = adapter.type;

      this.service.updateFacility(this.cmdbConfig).subscribe(
        (data)=>{
          this.loading = false;
          this.router.navigate(["/ui/nav/facility/cmdb/cmdb-list"]);
        },(error:HttpErrorResponse)=>{
          this.loading = false;
          if(error.status == 400 && error.error.message == "Invalid SSL Certificate"){
            this.verify = true;
            this.ignoreCertificatesModals = true;
            this.tip = error.error.message+". Are you sure you ignore the certificate check?"
          }else if(error.status == 400 && error.error.message == "Unknown Host"){
            this.operatingModals = true;
            this.tip = error.error.message+". Please check your serverIp. ";
          }else if(error.status == 401){
            this.operatingModals = true;
            this.tip = error.error.message+". Please check your userName or password. ";
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
    let verifyCert:boolean = this.editCMDBForm.get('verifyCert').value;
    if(verifyCert){
      this.editCMDBForm.get('verifyCert').setValue('false');
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
          this.adapterMap.set(element.subCategory,element);
        });
        let reqList:SubscribableOrPromise<any>[] = [];
        reqList.push(this.service.getDcimConfig(this.cmdbConfig.id));
        forkJoin(reqList).pipe(map((data)=>data)).subscribe((res)=>{
          res.forEach((element:FacilityModule) => {
            this.editCMDBForm.setValue(element);
            this.editCMDBForm.get('type').setValue(element.subCategory);
            let verifyCert:boolean = this.editCMDBForm.get('verifyCert').value;
            if(verifyCert){
              this.editCMDBForm.get('verifyCert').setValue('true');
            }else{
              this.editCMDBForm.get('verifyCert').setValue('false');
            }
            this.checkIsLabsDB();
          });
        })
      }
    )
  }
  ngOnInit() {
    this.cmdbConfig.id = this.activedRoute.snapshot.params['id'];
    if(this.cmdbConfig.id != null && this.cmdbConfig.id != ""){
      this.findAllAdapters();
    }
  }

}
