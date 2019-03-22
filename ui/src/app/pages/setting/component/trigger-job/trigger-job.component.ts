/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { SettingService } from '../../setting.service';


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
  temphumidityMapping:boolean=false;

  alertclose:boolean = true;
  alertType:string = "";
  alertcontent:string = "";

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
    }else if(jobname == "job2"){
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
    }else if(jobname == "job3"){
      this.temphumidityMapping = true;
      this.service.fullSyncTempAndHumiditySensors(true).subscribe(
        (data)=>{
          if(data.status == 201){
            this.temphumidityMapping = false;
            this.alertType = "alert-success";
            this.alertcontent = "Trigger Success";
            this.alertclose = false;
            setTimeout(() => {
              this.alertclose = true  
            },2000);
          }else{
            this.temphumidityMapping = false;
            this.alertType = "alert-danger";
            this.alertcontent = "Trigger Failed";
            this.alertclose = false;
            setTimeout(() => {
              this.alertclose = true  
            },5000);
          }
        },error=>{
          this.temphumidityMapping = false;
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

  ngOnInit() {
  }

}
