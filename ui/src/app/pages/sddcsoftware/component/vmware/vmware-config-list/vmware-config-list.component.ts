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
  basic:boolean = false;
  vmwareConfigId:string = '';
  clrAlertClosed:boolean = true;
  addsddc:string[]=["createSddcSoftwareConfig"];
  updatesddc:string[]=["updateSddcSoftwareConfig","readSddcSoftwareConfigByID"];
  deletesddc:string[]=["deleteSddcSoftwareConfig"];
  syncdata:string[]=["syncDataByServerId"];
  alertclose:boolean = true;
  alertType:string = "";
  alertcontent:string = "";

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
  createTime(time){
		var da = time;
	    da = new Date(da);
	    var year = da.getFullYear()+'-';
	    var month = da.getMonth()+1+'-';
	    var date = da.getDate();
	    return year+month+date;
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
  toEditvmwareConfig(){
    this.router.navigate(["/ui/nav/sddc/vmware/vmware-add"]);
  }
  onEdit(id){
    window.sessionStorage.setItem("editserverid",id);
    this.router.navigateByUrl("/ui/nav/sddc/vmware/vmware-edit");
  }
  confirm(){
    this.service.deleteVmwareConfig(this.vmwareConfigId).subscribe(data=>{
      
      if(data.status == 200){
        this.basic = false;
        this.getVmareConfigdatas(this.currentPage,this.pageSize)
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
    this.vmwareConfigId = "";
  }
  onDelete(id){
    this.basic = true;
    this.vmwareConfigId = id;
  
  }
  close(){
    this.alertclose = true
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