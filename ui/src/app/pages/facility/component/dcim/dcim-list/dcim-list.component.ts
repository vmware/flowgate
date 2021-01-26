/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DcimService } from '../dcim.service';
import { FacilityModule } from '../../../facility.module';
import { FacilityAdapterModule } from 'app/pages/setting/component/adaptertype/facility-adapter.module';
import { ClrDatagridStateInterface } from '@clr/angular';
@Component({
  selector: 'app-dcim-list',
  templateUrl: './dcim-list.component.html',
  styleUrls: ['./dcim-list.component.scss']
})
export class DcimListComponent implements OnInit {

  constructor(private service:DcimService,private router: Router) { }

  dcimConfigs = [];
  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '20';
  info:string='';
  disabled:String="";
  basic:boolean = false;
  dcimConfigId:string = '';
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
  types:string = "Nlyte,PowerIQ,OpenManage,OtherDCIM";
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
  showErrorMsg(dcim:any){
    this.isStatusErrorMsgAlertClose = false;
    this.statusErrorMsg = "The server "+dcim.name+" has an error:"+dcim.integrationStatus.detail
    this.editStatusDcimId = dcim.id;
  }
  statusMsgAlertClose(){
    this.isStatusErrorMsgAlertClose = true;
    this.editStatusDcimId = "";
  }
  fixError(){
    let id = this.editStatusDcimId;
    this.router.navigate(['/ui/nav/facility/dcim/dcim-edit',id]);
  }
  updateStatusResultClose(){
    this.updateStatusAlertclose = true;
  }
  updateDcimStatus(dcim:FacilityModule){
    var updateDcim:FacilityModule = new FacilityModule();
    updateDcim.id = dcim.id;
    if(dcim.integrationStatus.status == "ACTIVE"){
      updateDcim.integrationStatus = {
        "status":"PENDING",
        "detail":""
      }
    }else{
      updateDcim.integrationStatus = {
        "status":"ACTIVE",
        "detail":"",
        "retryCounter":0
      };
    }
    this.service.updateFacilityStatus(updateDcim).subscribe(
      (data)=>{
        this.updateStatusAlertType = "success";
        if(updateDcim.integrationStatus.status == "ACTIVE"){
          this.updateStatusAlertcontent = "The server "+dcim.name+" has been activated.";
        }else{
          this.updateStatusAlertcontent = "The server "+dcim.name+" has been suspended.";
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
  setInfo(){
    this.info=this.pageSize;
    this.refresh(this.currentState)
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.refresh(this.currentState)
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++
      this.refresh(this.currentState)
    }
  }
  loading:boolean = true;
  currentState:ClrDatagridStateInterface;
  totalItems:number = 0;
  refresh(state: ClrDatagridStateInterface){
    this.dcimConfigs = [];
    if (!state.page) {
      return;
    }
    this.currentState = state;
    this.getDcimConfigdatas(state.page.current,state.page.size);
  }
  getDcimConfigdatas(currentPage:number,pageSize:number){
    this.loading = true;
    this.dcimConfigs = [];
    this.service.getDcimConfigData(currentPage,pageSize,this.types).subscribe(
      (data)=>{
        this.loading = false;
        this.dcimConfigs = data['content'];
        this.dcimConfigs.forEach(element=>{
            this.checkStatus(element);
            if(element.type == 'OtherDCIM'){
              element.type = this.adapterMap.get(element.subCategory).displayName;
            }
        })
        this.totalItems = data['totalElements'];
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
    this.router.navigate(["/ui/nav/facility/dcim/dcim-add"]);
  }
  onEdit(id){
    this.router.navigate(['/ui/nav/facility/dcim/dcim-edit',id]);
  }
  confirm(){
    this.service.deleteDcimConfig(this.dcimConfigId).subscribe(
      data=>{
        this.basic = false;
        this.refresh(this.currentState)

    },error=>{
        this.clrAlertClosed = false;
    })
  }
  onClose(){
    this.basic = false;
  }
  cancel(){
    this.basic = false;
    this.dcimConfigId = "";
  }
  onDelete(id){
    this.basic = true;
    this.dcimConfigId = id;

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
  dcimAdapters:FacilityAdapterModule[] = [];
  adapterMap:Map<String,FacilityAdapterModule> = new Map<String,FacilityAdapterModule>();
  findAllAdapters(){
    this.service.findAllFacilityAdapters().subscribe(
      (allFacilityAdapters:FacilityAdapterModule[])=>{
        allFacilityAdapters.forEach(element => {
          if(element.type == "OtherDCIM"){
            this.dcimAdapters.push(element);
          }
        });
        this.dcimAdapters.forEach(element => {
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
