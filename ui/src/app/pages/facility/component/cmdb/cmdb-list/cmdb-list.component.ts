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

@Component({
  selector: 'app-cmdb-list',
  templateUrl: './cmdb-list.component.html',
  styleUrls: ['./cmdb-list.component.scss']
})
export class CmdbListComponent implements OnInit {

  constructor(private http:Http,private service:DcimService,private router: Router, private route: ActivatedRoute) { }
 
  dcimConfigs = [];
  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '5';
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
    this.dcimConfigs = [];
    this.service.getDcimConfigData(currentPage,pageSize).subscribe(
      (data)=>{if(data.status == 200){
            var types:string[]=["InfoBlox","OtherCMDB"]; 
            data.json().content.forEach(element=>{
              if(types.indexOf(element.type) != -1){
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
    this.router.navigate(["/ui/nav/facility/cmdb/cmdb-add"]);
  }
  onEdit(id){
    window.sessionStorage.setItem("editdcimconfigid",id);
    this.router.navigateByUrl("/ui/nav/facility/cmdb/cmdb-edit");
  }
  confirm(){
    this.service.deleteDcimConfig(this.dcimConfigId).subscribe(data=>{
      
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
     this.getVmareConfigdatas(this.currentPage,this.pageSize); 
  
  }

}
