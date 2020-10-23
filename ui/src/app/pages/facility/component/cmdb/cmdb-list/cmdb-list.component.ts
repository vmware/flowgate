/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import {Router,ActivatedRoute} from '@angular/router';
import { error } from 'util';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import { DcimService } from '../../dcim/dcim.service';
import { FacilityModule } from '../../../facility.module';
import { FacilityAdapterModule } from 'app/pages/setting/component/adaptertype/facility-adapter.module';

@Component({
  selector: 'app-cmdb-list',
  templateUrl: './cmdb-list.component.html',
  styleUrls: ['./cmdb-list.component.scss']
})
export class CmdbListComponent implements OnInit {

  constructor(private http:Http,private service:DcimService,private router: Router, private route: ActivatedRoute) { }
 
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
    window.sessionStorage.setItem("editdcimconfigid",this.editStatusDcimId);
    this.router.navigateByUrl("/ui/nav/facility/cmdb/cmdb-edit");
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
        if(data.status == 200){
          this.updateStatusAlertType = "alert-success";
          if(updateCmdb.integrationStatus.status == "ACTIVE"){
            this.updateStatusAlertcontent = "The server "+cmdb.name+" has been activated.";
          }else{
            this.updateStatusAlertcontent = "The server "+cmdb.name+" has been suspended.";
          }
          this.updateStatusAlertclose = false;
          setTimeout(() => {
            this.updateStatusAlertclose = true  
          },2000);
          this.getCMDBConfigdatas(this.currentPage,this.pageSize);
        }
      },error=>{
        this.updateStatusAlertType = "alert-danger";
        this.updateStatusAlertcontent = "Activation or suspension of the server failed.";
        this.updateStatusAlertclose = false;
        setTimeout(() => {
          this.updateStatusAlertclose = true  
        },2000);
        this.getCMDBConfigdatas(this.currentPage,this.pageSize);
      }
    )
  }

  setInfo(){
    this.info=this.pageSize;
    this.getCMDBConfigdatas(this.currentPage,this.pageSize)
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.getCMDBConfigdatas(this.currentPage,this.pageSize)
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++
      this.getCMDBConfigdatas(this.currentPage,this.pageSize)
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
  loading:boolean = true;
  getCMDBConfigdatas(currentPage,pageSize){
    this.loading = true;
    this.cmdbConfigs = [];
    this.service.getDcimConfigData(currentPage,pageSize,this.types).subscribe(
      (data)=>{
        if(data.status == 200){
            this.loading = false;
            this.cmdbConfigs =  data.json().content;
            this.cmdbConfigs.forEach(element=>{
                this.checkStatus(element);
                if(element.type == "OtherCMDB"){
                  element.type = this.adapterMap.get(element.subCategory).displayName;
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
  addDcimConfig(){
    this.router.navigate(["/ui/nav/facility/cmdb/cmdb-add"]);
  }
  onEdit(id){
    this.router.navigate(['/ui/nav/facility/cmdb/cmdb-edit',id]);
  }
  confirm(){
    this.service.deleteDcimConfig(this.cmdbConfigId).subscribe(data=>{
      
      if(data.status == 200){
        this.basic = false;
        this.getCMDBConfigdatas(this.currentPage,this.pageSize)
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
  cmdbAdapters:FacilityAdapterModule[] = [];
  adapterMap:Map<String,FacilityAdapterModule> = new Map<String,FacilityAdapterModule>();
  findAllAdapters(){
    this.service.findAllFacilityAdapters().subscribe(
      (data)=>{
        let allFacilityAdapters:FacilityAdapterModule[] = [];
        allFacilityAdapters = data.json();
        allFacilityAdapters.forEach(element => {
          if(element.type == "OtherCMDB"){
            this.cmdbAdapters.push(element);
          }
        });
        this.cmdbAdapters.forEach(element => {
          this.adapterMap.set(element.subCategory,element);
        });
        this.getCMDBConfigdatas(this.currentPage,this.pageSize); 
      }
    )
  }
  ngOnInit() {
    this.findAllAdapters();
  }

}
