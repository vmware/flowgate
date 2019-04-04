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
     window.sessionStorage.setItem("editserverid",this.editStatusSDDCId);
     this.router.navigateByUrl("/ui/nav/sddc/vmware/vmware-edit");
   }
   updateStatusResultClose(){
     this.updateStatusAlertclose = true;
   }

   updateSDDCStatus(sddc:SddcsoftwareModule){
    var toUpdateSddc:SddcsoftwareModule = new SddcsoftwareModule();
    toUpdateSddc.id = sddc.id;
    toUpdateSddc.type = sddc.type;
    toUpdateSddc.description = sddc.description
    toUpdateSddc.name = sddc.name;
    toUpdateSddc.userName = sddc.userName;
    toUpdateSddc.password = sddc.password;
    toUpdateSddc.serverURL = sddc.serverURL;
    toUpdateSddc.verifyCert = sddc.verifyCert;
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
    this.service.updateVmwareConfig(toUpdateSddc).subscribe(
      (data)=>{
        if(data.status == 200){
          this.updateStatusAlertType = "alert-success";
          if(toUpdateSddc.integrationStatus.status == "ACTIVE"){
            this.updateStatusAlertcontent = "The server "+toUpdateSddc.name+" has been activated.";
          }else{
            this.updateStatusAlertcontent = "The server "+toUpdateSddc.name+" has been suspended.";
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

  getVmareConfigdatas(currentPage,pageSize){
    this.service.getVmwareConfigData(currentPage,pageSize).subscribe(
      (data)=>{if(data.status == 200){
          
            this.vmwareConfigs = data.json().content
            this.currentPage = data.json().number+1;
            this.totalPage = data.json().totalPages
            if(this.totalPage == 1){
              this.disabled = "disabled";
            }else{
              this.disabled = "";
            }
      }
    })
  }
  addNewSddc(){
    this.router.navigate(["/ui/nav/sddc/vmware/vmware-add"]);
  }
  onEdit(id){
    window.sessionStorage.setItem("editserverid",id);
    this.router.navigateByUrl("/ui/nav/sddc/vmware/vmware-edit");
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