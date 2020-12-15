/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DcimService } from '../../dcim/dcim.service';
import { FacilityModule } from '../../../facility.module';
import { FacilityAdapterModule } from 'app/pages/setting/component/adaptertype/facility-adapter.module';
import { ClrDatagridStateInterface } from '@clr/angular';

@Component({
  selector: 'app-cmdb-list',
  templateUrl: './cmdb-list.component.html',
  styleUrls: ['./cmdb-list.component.scss']
})
export class CmdbListComponent implements OnInit {

  constructor(private service:DcimService,private router: Router) { }

  cmdbConfigs = [];
  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '20';
  info:string='';
  disabled:String="";
  basic:boolean = false;
  cmdbConfigId:string = '';
  clrAlertClosed:boolean = true;


  addfacility:string[]=["createFacilitySoftwareConfig"];
  updatefacility:string[]=["readFacilityByID","updateFacilitySoftwareConfig"];
  deletefacility:string[]=["deleteFacilitySoftwareConfig"];
  syncdata:string[]=["syncDataByServerId"];
  alertclose:boolean = true;
  alertType:string = "";
  alertcontent:string = "";

  updateStatusAlertclose:boolean = true;
  updateStatusAlertType:string = "";
  updateStatusAlertcontent:string = "";

  //for integrationStatus alert message
  isStatusErrorMsgAlertClose:boolean=true;
  editStatusDcimId:string = "";
  statusErrorMsg = "";
  dcimModule:FacilityModule = new FacilityModule();
  types:string = "InfoBlox,OtherCMDB,Labsdb";
  checkStatus(element:any):any{
    var status = {
      "status":"ACTIVE",
      "detail":""
    };
    if(element.integrationStatus == null){
      element.integrationStatus = status;
    }
    return element;
  }
  showErrorMsg(cmdb:any){
    this.isStatusErrorMsgAlertClose = false;
    this.statusErrorMsg = "The server "+cmdb.name+" has an error:"+cmdb.integrationStatus.detail
    this.editStatusDcimId = cmdb.id;
  }
  statusMsgAlertClose(){
    this.isStatusErrorMsgAlertClose = true;
    this.editStatusDcimId = "";
  }
  fixError(){
    this.router.navigate(['/ui/nav/facility/cmdb/cmdb-edit',this.editStatusDcimId]);
  }
  updateStatusResultClose(){
    this.updateStatusAlertclose = true;
  }
  updateCMDBStatus(cmdb:FacilityModule){
    var updateCmdb:FacilityModule = new FacilityModule();
    updateCmdb.id = cmdb.id;
    if(cmdb.integrationStatus.status == "ACTIVE"){
      updateCmdb.integrationStatus = {
        "status":"PENDING",
        "detail":""
      }
    }else{
      updateCmdb.integrationStatus = {
        "status":"ACTIVE",
        "detail":"",
        "retryCounter":0
      };
    }
    this.service.updateFacilityStatus(updateCmdb).subscribe(
      (data)=>{
          this.updateStatusAlertType = "success";
          if(updateCmdb.integrationStatus.status == "ACTIVE"){
            this.updateStatusAlertcontent = "The server "+cmdb.name+" has been activated.";
          }else{
            this.updateStatusAlertcontent = "The server "+cmdb.name+" has been suspended.";
          }
          this.updateStatusAlertclose = false;
          setTimeout(() => {
            this.updateStatusAlertclose = true
          },2000);
          this.refresh(this.currentState);
      },error=>{
        this.updateStatusAlertType = "danger";
        this.updateStatusAlertcontent = "Activation or suspension of the server failed.";
        this.updateStatusAlertclose = false;
        setTimeout(() => {
          this.updateStatusAlertclose = true
        },2000);
        this.refresh(this.currentState);
      }
    )
  }

  loading:boolean = true;
  currentState:ClrDatagridStateInterface;
  totalItems:number = 0;
  refresh(state: ClrDatagridStateInterface){
    this.cmdbConfigs = [];
    if (!state.page) {
      return;
    }
    this.currentState = state;
    this.getCMDBConfigdatas(state.page.current,state.page.size);
  }
  getCMDBConfigdatas(currentPage,pageSize){
    this.loading = true;
    this.cmdbConfigs = [];
    this.service.getDcimConfigData(currentPage,pageSize,this.types).subscribe(
      (data)=>{
        this.loading = false;
        this.cmdbConfigs = data['content'];
        this.totalItems = data['totalElements'];
        this.cmdbConfigs.forEach(element=>{
            this.checkStatus(element);
            if(element.type == "OtherCMDB"){
              element.type = this.adapterMap.get(element.subCategory).displayName;
            }
        })
    },(error)=>{
        this.loading = false;
        this.alertType = "danger";
        this.alertcontent = "Internal error";
        if(error._body != null && error.status != "0"){
          this.alertcontent = error.json().message;
        }
        this.alertclose = false;
      })
  }
  addDcimConfig(){
    this.router.navigate(["/ui/nav/facility/cmdb/cmdb-add"]);
  }
  onEdit(id){
    this.router.navigate(['/ui/nav/facility/cmdb/cmdb-edit',id]);
  }
  confirm(){
    this.service.deleteDcimConfig(this.cmdbConfigId).subscribe(
      data=>{
        this.basic = false;
        this.refresh(this.currentState);
      },error=>{
        this.clrAlertClosed = false;
    })
  }
  onClose(){
    this.basic = false;
  }
  cancel(){
    this.basic = false;
    this.cmdbConfigId = "";
  }
  onDelete(id){
    this.basic = true;
    this.cmdbConfigId = id;

  }
  close(){
    this.alertclose = true;
  }
  syncData(id:string,url:string){
    this.service.syncData(id).subscribe(
      (data)=>{
        this.alertType = "success";
        this.alertcontent = "The sync job has been scheduled.";
        this.alertclose = false;
        setTimeout(() => {
          this.alertclose = true
        },2000);
      },error=>{
        this.alertType = "danger";
        this.alertcontent = "Failed to sync data for " +url;
        this.alertclose = false;
        setTimeout(() => {
          this.alertclose = true
        },2000);
      }
    )
  }
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
        this.cmdbAdapters.forEach(element => {
          this.adapterMap.set(element.subCategory,element);
        });
        this.refresh(this.currentState);
      }
    )
  }
  ngOnInit() {
    this.findAllAdapters();
  }

}
