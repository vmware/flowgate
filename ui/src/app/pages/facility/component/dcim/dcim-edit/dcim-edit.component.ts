/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { DcimService } from '../dcim.service';
import {Router,ActivatedRoute} from '@angular/router';
import { FacilityModule } from '../../../facility.module';
import { FacilityAdapterModule } from 'app/pages/setting/component/adaptertype/facility-adapter.module';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin, SubscribableOrPromise } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
@Component({
  selector: 'app-dcim-edit',
  templateUrl: './dcim-edit.component.html',
  styleUrls: ['./dcim-edit.component.scss']
})
export class DcimEditComponent implements OnInit {

  editDCIMForm:FormGroup;
  constructor(private service:DcimService,private router:Router,private activedRoute:ActivatedRoute,private fb: FormBuilder) {
    this.editDCIMForm = this.fb.group({
      type: [{value:'',disabled: true}, [
        Validators.required
      ]],
      serverURL: [{value:'',disabled: true}, [
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
      let advanceSetting:any = this.dcimConfig.advanceSetting;
      this.dcimConfig =this.editDCIMForm.value;
      let adapter:FacilityAdapterModule = this.adapterMap.get(this.editDCIMForm.get('type').value);
      this.dcimConfig.subCategory = adapter.subCategory;
      this.dcimConfig.advanceSetting = advanceSetting;
      this.service.updateFacility(this.dcimConfig).subscribe(
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
    this.editDCIMForm.get('verifyCert').setValue('false');
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
  predefinedType:string[] = ['Nlyte','PowerIQ','OpenManage'];
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
        let openManage:FacilityAdapterModule = new FacilityAdapterModule();
        openManage.displayName = "OpenManage";
        openManage.subCategory = "OpenManage";
        openManage.type = "OpenManage";
        this.dcimAdapters.push(openManage);
        this.dcimAdapters.push(powerIQ);
        this.dcimAdapters.forEach(element => {
          this.adapterMap.set(element.subCategory,element);
        });

        let reqList:SubscribableOrPromise<any>[] = [];
        reqList.push(this.service.getDcimConfig(this.dcimConfig.id));
        forkJoin(reqList).pipe(map((data)=>data)).subscribe((res)=>{
          res.forEach((element:FacilityModule) => {
            this.dcimConfig.advanceSetting = element.advanceSetting;
            this.editDCIMForm.setValue(element);
            if(this.predefinedType.indexOf(element.type) == -1){
              this.editDCIMForm.get('type').setValue(element.subCategory);
            }
            let verifyCert:boolean = this.editDCIMForm.get('verifyCert').value;
            if(verifyCert){
              this.editDCIMForm.get('verifyCert').setValue('true');
            }else{
              this.editDCIMForm.get('verifyCert').setValue('false');
            }
          });
        })
      }
    )
  }

  ngOnInit() {
    this.dcimConfig.id = this.activedRoute.snapshot.params['id'];
    this.dcimConfig.advanceSetting ={
      DateFormat:"",
      TimeZone:"",
      PDU_POWER_UNIT:"",
      PDU_AMPS_UNIT:"",
      PDU_VOLT_UNIT:"",
      TEMPERATURE_UNIT:"",
      HUMIDITY_UNIT:""
    }
    if(this.dcimConfig.id != null && this.dcimConfig.id != ""){
      this.findAllAdapters();
    }
  }

}
