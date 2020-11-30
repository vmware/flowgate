/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import {Router,ActivatedRoute} from '@angular/router';
import { SettingService } from '../../setting.service';
@Component({
  selector: 'app-sensorsetting-list',
  templateUrl: './sensorsetting-list.component.html',
  styleUrls: ['./sensorsetting-list.component.scss']
})
export class SensorsettingListComponent implements OnInit {
  constructor(private data:SettingService,private router: Router) { }
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

  getsensorsettingdatas(currentPage,pageSize){
    this.data.getsensorsettingData(currentPage,pageSize).subscribe(
      (data)=>{
        this.sensorsettings = data['content'];
        this.currentPage = data['number']+1;
        this.totalPage = data['totalPages']
        if(this.totalPage == 1){
          this.disabled = "disabled";
        }else{
          this.disabled = "";
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
    this.data.deletesensorsetting(this.sensorsettingId).subscribe(
      data=>{
        this.basic = false;
        this.getsensorsettingdatas(this.currentPage,this.pageSize)
    }, error=>{
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
