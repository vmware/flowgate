/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import {Router,ActivatedRoute} from '@angular/router';
import { error } from 'util';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import { DcimService } from '../dcim.service';
import { FacilityModule } from '../../../facility.module';
@Component({
  selector: 'app-dcim-list',
  templateUrl: './dcim-list.component.html',
  styleUrls: ['./dcim-list.component.scss']
})
export class DcimListComponent implements OnInit {

  constructor(private http:Http,private service:DcimService,private router: Router, private route: ActivatedRoute) { }
 
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
  operationDcim ={};
  dcimModule:FacilityModule = new FacilityModule();
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
    this.operationDcim = dcim;
  }
  statusMsgAlertClose(){
    this.isStatusErrorMsgAlertClose = true;
    this.editStatusDcimId = "";
    this.operationDcim = {};
  }
  fixError(){
    window.sessionStorage.setItem("editdcimconfigid",this.editStatusDcimId);
    this.router.navigateByUrl("/ui/nav/facility/dcim/dcim-edit");
  }
  updateStatusResultClose(){
    this.updateStatusAlertclose = true;
  }
  updateDcimStatus(dcim:FacilityModule){
    var updateDcim:FacilityModule = new FacilityModule();
    updateDcim.id = dcim.id;
    updateDcim.type = dcim.type;
    updateDcim.description = dcim.description
    updateDcim.name = dcim.name;
    updateDcim.userName = dcim.userName;
    updateDcim.password = dcim.password;
    updateDcim.serverURL = dcim.serverURL;
    updateDcim.advanceSetting = dcim.advanceSetting;
    updateDcim.verifyCert = dcim.verifyCert;
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
    this.service.updateFacility(updateDcim).subscribe(
      (data)=>{
        if(data.status == 200){
          this.updateStatusAlertType = "alert-success";
          if(updateDcim.integrationStatus.status == "ACTIVE"){
            this.updateStatusAlertcontent = "The server "+updateDcim.name+" has been activated.";
          }else{
            this.updateStatusAlertcontent = "The server "+updateDcim.name+" has been suspended.";
          }
          this.updateStatusAlertclose = false;
          setTimeout(() => {
            this.updateStatusAlertclose = true  
          },2000);
          this.getDcimConfigdatas(this.currentPage,this.pageSize);
        }
      },error=>{
        this.updateStatusAlertType = "alert-danger";
        this.updateStatusAlertcontent = "Activation or suspension of the server failed.";
        this.updateStatusAlertclose = false;
        setTimeout(() => {
          this.updateStatusAlertclose = true  
        },2000);
        this.getDcimConfigdatas(this.currentPage,this.pageSize);
      }
    )
  }
  setInfo(){
    this.info=this.pageSize;
    this.getDcimConfigdatas(this.currentPage,this.pageSize)
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.getDcimConfigdatas(this.currentPage,this.pageSize)
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++
      this.getDcimConfigdatas(this.currentPage,this.pageSize)
    }
  }
  createTime(time){
		var da = time;
	    da = new Date(da);
	    var year = da.getFullYear()+'-';
	    var month = da.getMonth()+1+'-';
	    var date = da.getDate();
	    return year+month+date;
	}
  getDcimConfigdatas(currentPage,pageSize){
    this.dcimConfigs = [];
    this.service.getDcimConfigData(currentPage,pageSize).subscribe(
      (data)=>{if(data.status == 200){     
            var types:string[]=["Nlyte","PowerIQ","OtherDCIM"]; 
            data.json().content.forEach(element=>{
              if(types.indexOf(element.type) != -1){                 
                this.checkStatus(element);
                this.dcimConfigs.push(element);
              }
            })
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
  addDcimConfig(){
    this.router.navigate(["/ui/nav/facility/dcim/dcim-add"]);
  }
  onEdit(id){
    window.sessionStorage.setItem("editdcimconfigid",id);
    this.router.navigateByUrl("/ui/nav/facility/dcim/dcim-edit");
  }
  confirm(){
    this.service.deleteDcimConfig(this.dcimConfigId).subscribe(data=>{
      
      if(data.status == 200){
        this.basic = false;
        this.getDcimConfigdatas(this.currentPage,this.pageSize)
      }else{
        this.clrAlertClosed = false;
      }
    },
    error=>{
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
     this.getDcimConfigdatas(this.currentPage,this.pageSize); 
  
  }

}
