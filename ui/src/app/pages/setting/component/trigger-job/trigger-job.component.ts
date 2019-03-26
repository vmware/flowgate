/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { SettingService } from '../../setting.service';
import { NodeLogger } from '@angular/core/src/view';


@Component({
  selector: 'app-trigger-job',
  templateUrl: './trigger-job.component.html',
  styleUrls: ['./trigger-job.component.scss']
})
export class TriggerJobComponent implements OnInit {

  constructor(private service:SettingService) { }
  triggerTipBox:boolean=false;
  syncData:string[]=["startFullMappingAggregation","generateServerPDUMapping"];
  syncUnMappedServers:string[]=["readUnMappedServers"];
  unmappedservershow:boolean=false;
  jobs=[
    {
      "id":"job1",
      "jobName":"Aggregate Server Mappings job",
      "description":"Manually trigger the aggregate Server mapping jobs. It will merge mappings between different SDDC systems and Facility system into one item. The merge strategy is try it's best to merge. This job will also triggered automatically daily."
    },
    {
      "id":"job2",
      "jobName":"Trigger aggregate PDUs and Server Mapping job",
      "description":"Manually Trigger the aggregate PDUs and Server mapping job. It will try to find out the possible PDUs that a server is connected to. The job will automatically executed by daily."
    }
  ]
  servers=[];
  serverloading:boolean=false;
 
  serverMapping:boolean=false;
  pduMapping:boolean=false;

  alertclose:boolean = true;
  alertType:string = "";
  alertcontent:string = "";
  systemDetail:boolean = false;
  serverDetail:boolean = false;
  sensorDetail:boolean = false;
  assetsNum:Number = 0;
  facilitySystemNum:Number = 0;
  categoryIsServerNum:Number = 0;
  categoryIsPduNum:Number = 0;
  categoryIsCabinetNum:Number = 0;
  categoryIsSwitchNum:Number = 0;
  categoryIsSensorNum:Number = 0;
  categoryIsUpsNum:Number = 0;
  userNum:Number = 0;
  sddcServerNum:Number = 0;
  sddcIntegrationNum:Number = 0;
  sddcIntegrationVcNum:Number = 0;
  sddcIntegrationVroNum:Number = 0;
  subCategoryIsHumidity:Number = 0;
  subCategoryIsTemperature:Number = 0;
  subCategoryIsAirFlow:Number = 0;
  subCategoryIsSmoke:Number = 0;
  subCategoryIsWater:Number = 0;
  dashBoardSystemNlyteDetail = [];
  dashBoardSystemPowerIqDetail = [];
  dashBoardSystemSddcServerVcDetail = [];
  dashBoardSystemSddcServerVroDetail = [];

  close(){
    this.alertclose = true
  }
  trigger(jobname:string){

    if(jobname == "job1"){
      this.serverMapping = true;
      this.service.mergeserverMapping().subscribe(
        (data)=>{
          if(data.status == 201){
            this.serverMapping = false;
            this.alertType = "alert-success";
            this.alertcontent = "Trigger Success";
            this.alertclose = false;
            setTimeout(() => {
              this.alertclose = true  
            },2000);
          }else{
            this.serverMapping = false;
            this.alertType = "alert-danger";
            this.alertcontent = "Trigger Failed";
            this.alertclose = false;
            setTimeout(() => {
              this.alertclose = true  
            },5000);
          }
        },error=>{
          this.serverMapping = false;
          this.alertType = "alert-danger";
            this.alertcontent = "Trigger Failed";
            this.alertclose = false;
            setTimeout(() => {
              this.alertclose = true  
            },5000);
        }
      )
    }else if(jobname = "job2"){
      this.pduMapping = true;
      this.service.mergePduServerMapping().subscribe(
        (data)=>{
          if(data.status == 201){
            this.pduMapping = false;
            this.alertType = "alert-success";
            this.alertcontent = "Trigger Success";
            this.alertclose = false;
            setTimeout(() => {
              this.alertclose = true  
            },2000);
          }else{
            this.pduMapping = false;
            this.alertType = "alert-danger";
            this.alertcontent = "Trigger Failed";
            this.alertclose = false;
            setTimeout(() => {
              this.alertclose = true  
            },5000);
          }
        },error=>{
          this.pduMapping = false;
          this.alertType = "alert-danger";
            this.alertcontent = "Trigger Failed";
            this.alertclose = false;
            setTimeout(() => {
              this.alertclose = true  
            },5000);
        }
      )
    }
  }

  showSystemDetail(){
    this.systemDetail = true;
  }
  systemConfirm(){
    this.systemDetail = false;
  }

  showServerDetail(){
    this.serverDetail = true;
  }
  serverConfirm(){
    this.serverDetail = false;
  }
  showSensorDetail(){
    this.sensorDetail = true;
  }
  sensorConfirm(){
    this.sensorDetail = false;
  }
  
  getunmappedservers(){
    this.serverloading = true;
    this.unmappedservershow = false;
    setTimeout(() => {
      this.unmappedservershow = true;
      this.service.getUnmappedserver().subscribe(
        (data)=>{
          if(data.status == 200){
            this.servers = data.json();
            this.serverloading = false;
          }
        }
      )
    },2000);

  }
  getFirstPageData(){
    this.service.getFirstPageDataServer().subscribe(
      (data)=>{
        if(data.status == 200){
          this.assetsNum = data.json().assetsNum;
          this.facilitySystemNum = data.json().facilitySystemNum;
          this.categoryIsServerNum = data.json().categoryIsServerNum;
          this.categoryIsPduNum = data.json().categoryIsPduNum;
          this.categoryIsCabinetNum = data.json().categoryIsCabinetNum;
          this.categoryIsSwitchNum = data.json().categoryIsSwitchNum;
          this.categoryIsSensorNum  = data.json().categoryIsSensorNum;
          this.categoryIsUpsNum  = data.json().categoryIsUpsNum;
          this.userNum  = data.json().userNum;
          this.sddcServerNum  = data.json().sddcServerNum;
          this.sddcIntegrationNum  = data.json().sddcIntegrationNum;
          this.sddcIntegrationVcNum  = data.json().sddcIntegrationVcNum;
          this.sddcIntegrationVroNum  = data.json().sddcIntegrationVroNum;
          this.subCategoryIsAirFlow = data.json().subCategoryIsAirFlow;
          this.subCategoryIsHumidity = data.json().subCategoryIsHumidity;
          this.subCategoryIsSmoke = data.json().subCategoryIsSmoke;
          this.subCategoryIsTemperature = data.json().subCategoryIsTemperature;
          this.subCategoryIsWater = data.json().subCategoryIsWater;
          this.dashBoardSystemNlyteDetail = data.json().dashBoardSystemNlyteDetail;
          this.dashBoardSystemPowerIqDetail = data.json().dashBoardSystemPowerIqDetail;
          this.dashBoardSystemSddcServerVcDetail = data.json().dashBoardSystemSddcServerVcDetail;
          this.dashBoardSystemSddcServerVroDetail = data.json().dashBoardSystemSddcServerVroDetail;
        }
      }
    )
  }

  getAllDataWhenClickTab(){
    this.getFirstPageData();
  }
  ngOnInit() {
    this.getFirstPageData();
  }

}