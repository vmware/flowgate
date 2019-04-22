/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import {Router,ActivatedRoute} from '@angular/router';
import { error } from 'util';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import { SettingService } from '../../setting.service';
@Component({
  selector: 'app-sensorsetting-list',
  templateUrl: './sensorsetting-list.component.html',
  styleUrls: ['./sensorsetting-list.component.scss']
})
export class SensorsettingListComponent implements OnInit {
  constructor(private http:Http,private data:SettingService,private router: Router, private route: ActivatedRoute) { }
  sensorsettings = [];
  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '5';
  info:string='';
  disabled:String="";
  basic:boolean = false;
  sensorsettingId:string = '';
  clrAlertClosed:boolean = true;

  addsensorsetting:string[]=["createSensorSetting"];
  updatesensorsetting:string[]=["readSensorSettingByID","updateSensorSetting"];
  deletesensorsetting:string[]=["deleteSensorSetting"];

  setInfo(){
    this.info=this.pageSize;
    this.getsensorsettingdatas(this.currentPage,this.pageSize)
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.getsensorsettingdatas(this.currentPage,this.pageSize)
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++
      this.getsensorsettingdatas(this.currentPage,this.pageSize)
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
  getsensorsettingdatas(currentPage,pageSize){
    this.data.getsensorsettingData(currentPage,pageSize).subscribe(
      (data)=>{if(data.status == 200){
            this.sensorsettings = data.json().content;
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
  toEditSensorsetting(){
    this.router.navigate(["/ui/nav/setting/sensorsetting-add"]);
  }
  onEdit(id){
    this.router.navigate(['/ui/nav/setting/sensorsetting-edit',id]);
  }
  confirm(){
    this.data.deletesensorsetting(this.sensorsettingId).subscribe(data=>{
      if(data.status == 200){
        this.basic = false;
        this.getsensorsettingdatas(this.currentPage,this.pageSize)
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
    this.sensorsettingId = "";
  }
  onDelete(id){
    this.basic = true;
    this.sensorsettingId = id;
  }
  ngOnInit() {
     this.getsensorsettingdatas(this.currentPage,this.pageSize); 
  
  }
}
