/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import {Router,ActivatedRoute} from '@angular/router';
import { error } from 'util';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import { VmwareService } from '../vmware.service';
import { SddcsoftwareModule } from '../../../sddcsoftware.module';
@Component({
  selector: 'app-vmware-config-list',
  templateUrl: './vmware-config-list.component.html',
  styleUrls: ['./vmware-config-list.component.scss']
})
export class VmwareConfigListComponent implements OnInit {

  constructor(private http:Http,private service:VmwareService,private router: Router, private route: ActivatedRoute) { }

  vmwareConfigs = [];
  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '5';
  info:string='';
  disabled:String="";
  deleteOperationConfirm:boolean = false;
  vmwareConfigId:string = '';
  deleteOperationAlertClosed:boolean = true;
  addsddc:string[]=["createSddcSoftwareConfig"];
  updatesddc:string[]=["updateSddcSoftwareConfig","readSddcSoftwareConfigByID"];
  deletesddc:string[]=["deleteSddcSoftwareConfig"];
  syncdata:string[]=["syncDataByServerId"];
  alertclose:boolean = true;
  alertType:string = "";
  alertcontent:string = "";

  updateStatusAlertclose:boolean = true;
  updateStatusAlertType:string = "";
  updateStatusAlertcontent:string = "";

   //for integrationStatus alert message
   isStatusErrorMsgAlertClose:boolean=true;
   editStatusSDDCId:string = "";
   statusErrorMsg = "";
   sddc:SddcsoftwareModule = new SddcsoftwareModule();
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

   checkIsMp(config:SddcsoftwareModule){
    if(config.type == "VROPSMP"){
      return true;
    }
    return false;
   }

   showErrorMsg(sddc:SddcsoftwareModule){
     this.isStatusErrorMsgAlertClose = false;
     this.statusErrorMsg = "The server "+sddc.name+" has an error:"+sddc.integrationStatus.detail
     this.editStatusSDDCId = sddc.id;
   }
   statusMsgAlertClose(){
     this.isStatusErrorMsgAlertClose = true;
     this.editStatusSDDCId = "";
   }
   fixError(){
     this.router.navigate(['/ui/nav/sddc/vmware/vmware-edit',this.editStatusSDDCId]);
   }
   updateStatusResultClose(){
     this.updateStatusAlertclose = true;
   }

   updateSDDCStatus(sddc:SddcsoftwareModule){
    var toUpdateSddc:SddcsoftwareModule = new SddcsoftwareModule();
    toUpdateSddc.id = sddc.id;
    if(sddc.integrationStatus.status == "ACTIVE"){
      toUpdateSddc.integrationStatus = {
        "status":"PENDING",
        "detail":""
      }
    }else{
      toUpdateSddc.integrationStatus = {
        "status":"ACTIVE",
        "detail":"",
        "retryCounter":0
      };
    }
    this.service.updateStatus(toUpdateSddc).subscribe(
      (data)=>{
        if(data.status == 200){
          this.updateStatusAlertType = "alert-success";
          if(toUpdateSddc.integrationStatus.status == "ACTIVE"){
            this.updateStatusAlertcontent = "The server "+sddc.name+" has been activated.";
          }else{
            this.updateStatusAlertcontent = "The server "+sddc.name+" has been suspended.";
          }
          this.updateStatusAlertclose = false;
          setTimeout(() => {
            this.updateStatusAlertclose = true  
          },2000);
          this.getVmareConfigdatas(this.currentPage,this.pageSize);
        }
      },error=>{
        this.updateStatusAlertType = "alert-danger";
        this.updateStatusAlertcontent = "Activation or suspension of the server failed.";
        this.updateStatusAlertclose = false;
        setTimeout(() => {
          this.updateStatusAlertclose = true  
        },2000);
        this.getVmareConfigdatas(this.currentPage,this.pageSize);
      }
    )
  }

  setInfo(){
    this.info=this.pageSize;
    this.getVmareConfigdatas(this.currentPage,this.pageSize)
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.getVmareConfigdatas(this.currentPage,this.pageSize)
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++
      this.getVmareConfigdatas(this.currentPage,this.pageSize)
    }
  }
  loading:boolean = true;
  getVmareConfigdatas(currentPage,pageSize){
    this.loading = true;
    this.service.getVmwareConfigData(currentPage,pageSize).subscribe(
      (data)=>{
        if(data.status == 200){
          this.vmwareConfigs = data.json().content;
          this.vmwareConfigs.forEach(element=>{  
          if(element.type == "VRO"){
            element.type = "vROps";
          }else if(element.type == "VCENTER"){
            element.type ="vCenter";
          }  
          this.checkStatus(element);
        })
        this.currentPage = data.json().number+1;
        this.totalPage = data.json().totalPages;
        if(this.totalPage == 1){
          this.disabled = "disabled";
        }else{
          this.disabled = "";
        }
        this.loading = false;
      }
    },
    (error)=>{
      this.loading = false;
      this.alertType = "alert-danger";
      this.alertcontent = "Internal error";
      if(error._body != null && error.status != "0"){
        this.alertcontent = error.json().message;
      }
      this.alertclose = false;
    })
  }
  addNewSddc(){
    this.router.navigate(["/ui/nav/sddc/vmware/vmware-add"]);
  }
  onEdit(id){
    this.router.navigate(['/ui/nav/sddc/vmware/vmware-edit',id]);
  }
  confirmDelete(){
    this.service.deleteVmwareConfig(this.vmwareConfigId).subscribe(data=>{
      
      if(data.status == 200){
        this.deleteOperationConfirm = false;
        this.getVmareConfigdatas(this.currentPage,this.pageSize)
      }else{
        this.deleteOperationAlertClosed = false;
      }
    },
    error=>{
      this.deleteOperationAlertClosed = false;
    })
  }
 
  closeModalOfDeleteOperation(){
    this.deleteOperationConfirm = false;
    this.vmwareConfigId = "";
  }
  onDelete(id){
    this.deleteOperationConfirm = true;
    this.vmwareConfigId = id;
  }

  syncData(id:string,url:string){
    this.service.syncData(id).subscribe(
      (data)=>{
        if(data.status == 201){
            this.alertType = "alert-success";
            this.alertcontent = "The sync job has been scheduled.";
            this.alertclose = false;
            setTimeout(() => {
              this.alertclose = true  
            },2000);
        }else{
          this.alertType = "alert-danger";
          this.alertcontent = "Failed to sync data for " +url;
          this.alertclose = false;
          setTimeout(() => {
            this.alertclose = true  
          },2000);
        }
      },error=>{
        this.alertType = "alert-danger";
        this.alertcontent = "Failed to sync data for " +url;
        this.alertclose = false;
        setTimeout(() => {
          this.alertclose = true  
        },2000);
      }
    )
  }
  ngOnInit() {
     this.getVmareConfigdatas(this.currentPage,this.pageSize); 
  
  }
}