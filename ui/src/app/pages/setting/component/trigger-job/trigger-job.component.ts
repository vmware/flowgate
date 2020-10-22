/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit, ViewChild } from '@angular/core';
import { SettingService } from '../../setting.service';
import { NgForm } from '@angular/forms';
import { HostNameAndIpmappingModule } from '../../host-name-and-ipmapping/host-name-and-ipmapping.module';
import { fromEvent } from 'rxjs/observable/fromEvent';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { of } from 'rxjs/observable/of';
import { FileUploader, ParsedResponseHeaders, FileItem } from 'ng2-file-upload';
import {environment} from '../../../../../environments/environment.prod';
import { AuthenticationService } from '../../../auth/authenticationService';

@Component({
  selector: 'app-trigger-job',
  templateUrl: './trigger-job.component.html',
  styleUrls: ['./trigger-job.component.scss']
})
export class TriggerJobComponent implements OnInit {
  constructor(private service:SettingService, private auth:AuthenticationService) { 
    this.uploader.onSuccessItem = this.successItem.bind(this);
    this.uploader.onErrorItem = this.errorItem.bind(this);
    // this.uploader.onBuildItemForm = this.onBuildItemForm.bind(this);
  }
  triggerTipBox:boolean=false;
  syncData:string[]=["startFullMappingAggregation","generateServerPDUMapping"];
  syncUnMappedServers:string[]=["readUnMappedServers"];
  integrationSummary:string[]=["readSystemSummary"];
  assetTopology:string[]=["readAsset","readSddcSoftwareConfigByVC","readMappingsByVCID"];
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
  systemSummary:boolean = false;
  serverSummary:boolean = false;
  sensorSummary:boolean = false;
  expiredTimeRange:any;
  toUpdateExpiredTimeRange:any = "";
  updateExpiredTime:boolean = false;
  validExpiredTime:boolean = false;
  @ViewChild("timeForm") timeForm: NgForm;
  userFormRef:NgForm;
  errorShow:boolean = false;
  errorMsg:string = "";
  flowgateSummery={
    "assetsNum": 0,
    "facilitySystemNum": 0,
    "serverNum": 0,
    "pduNum": 0,
    "cabinetNum": 0,
    "switchNum": 0,
    "sensorNum": 0,
    "categoryIsUpsNum": 0,
    "userNum": 0,
    "sddcServerNum": 0,
    "sddcIntegrationNum": 0,
    "vcNum": 0,
    "vroNum": 0,
    "humiditySensorNum": 0,
    "temperatureSensorNum": 0,
    "airFlowSensorNum": 0,
    "smokeSensorNum": 0,
    "waterSensorNum": 0,
    "nlyteSummary": [],
    "powerIqSummary": [],
    "vcSummary": [],
    "vroSummary": []
  }

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

  updateExpiredTimeRange(){
    this.updateExpiredTime = true;
  }
  getValidationState(){
    return this.validExpiredTime;
  }
  reset(){
    this.toUpdateExpiredTimeRange = this.expiredTimeRange;
    this.updateExpiredTime = false;
    this.validExpiredTime = false;
    this.errorShow = false;
    this.errorMsg = "";
  }
  save(){
    let tosaveTime = this.toUpdateExpiredTimeRange*24*3600*1000;
    this.service.updatesTimeRange(tosaveTime).subscribe(
      (data)=>{
        if(data.status == 200){
          this.updateExpiredTime = false;
          this.errorShow = false;
          this.errorMsg = "";
          this.getExpiredTimeRange();
        }
      },error=>{
        this.errorShow = true;
        this.errorMsg = error.json().message;
      }
    )
  }
  handleValidation(key: string, flag: boolean): void {
    if(flag){
      if(this.toUpdateExpiredTimeRange >= 90){
        this.validExpiredTime = false;
        }else{
          this.validExpiredTime = true;
        }
    }
  }

  getExpiredTimeRange(){
    this.service.getExpiredTimeRange().subscribe(
      (data)=>{
        this.expiredTimeRange = data.text();
        this.expiredTimeRange = this.expiredTimeRange/(3600*1000*24)
        this.toUpdateExpiredTimeRange = this.expiredTimeRange;
      }
      )
  }

  showSystemSummary(){
    this.systemSummary = true;
  }
  systemConfirm(){
    this.systemSummary = false;
  }
  showServerSummary(){
    this.serverSummary = true;
  }
  serverConfirm(){
    this.serverSummary = false;
  }
  showSensorSummary(){
    this.sensorSummary = true;
  }
  sensorConfirm(){
    this.sensorSummary = false;
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
    this.service.getSystemSummaryData().subscribe(
      (data)=>{
        if(data.status == 200){
          this.flowgateSummery = data.json();
        }
      }
    )
  }

  getAllDataWhenClickTab(){
    this.getFirstPageData();
  }

  //assetNameAndIPMapping
  hostNameAndIPMappings:HostNameAndIpmappingModule[] = [];
  selectedHostNameAndIPMappings:HostNameAndIpmappingModule[] = [];
  pageSize:number = 10;
  pageNumber:number = 1;
  mappingsTotalPage:number = 0;
  loading:boolean = false;
  disabled:string="";
  selectedAssetName:string="";
  changePageSize(){
    this.getHostNameAndIPMappings(this.pageSize,this.pageNumber,this.searchIP)
  }
  previous(){
    if(this.pageNumber>1){
      this.pageNumber--;
      this.getHostNameAndIPMappings(this.pageSize,this.pageNumber,this.searchIP)
    }
  }
  next(){
    if(this.pageNumber < this.mappingsTotalPage){
      this.pageNumber++
      this.getHostNameAndIPMappings(this.pageSize,this.pageNumber,this.searchIP)
    }
  }
  searchIP:string = null;
  searchBtnState:boolean = false;
  searchBtnDisabled:boolean = false;
  searchErrorShow:boolean = false;
  searchErrorMsg:string = "";
  search(){
    if(this.searchIP != null){
      let ip = this.searchIP.trim();
      if(ip == ""){
        this.getHostNameAndIPMappings(this.pageSize,this.pageNumber,null);
      }else{
        if(ip.match(this.ipRegx)){
          this.pageSize = 10;
          this.pageNumber = 1;
          this.searchErrorShow = false;
          this.getHostNameAndIPMappings(this.pageSize,this.pageNumber,ip);
        }else{
          this.searchErrorShow = true;
          this.searchErrorMsg = "Please enter a valid IP";
        }
      }
    }
  }
  getHostNameAndIPMappings(pagesize:number,pagenumber:number,searchIP:string){
    this.loading = true;
    this.service.getHostNameAndIPMapping(pagenumber,pagesize,searchIP).subscribe(
      data=>{
        if(data.status == 200){
          this.loading = false;
          if(data.text() != null){
            this.hostNameAndIPMappings = data.json().content;
            this.mappingsTotalPage = data.json()
            this.pageNumber = data.json().number+1;
            this.mappingsTotalPage = data.json().totalPages
            if(this.mappingsTotalPage == 1){
              this.disabled = "disabled";
            }else{
              this.disabled = "";
            }
          }
        }
      },(error)=>{
        this.loading = false;
      }
    )
  }
  deleteHostNameAndIPMapping:boolean=false;
  AddHostNameAndIPMapping:boolean = false;
  hostNameAndIPMapping:HostNameAndIpmappingModule = new HostNameAndIpmappingModule();
  editHostNameAndIPMapping:boolean = false;
  onDelete(){
    this.deleteHostNameAndIPMapping = true;
  }
  cancelDelete(){
    this.deleteHostNameAndIPMapping = false;
  }
  confirmDelete(){
    this.deleteHostNameAndIPMapping = false;
    this.service.deleteHostNameAndIPMapping(this.selectedHostNameAndIPMappings[0].id).subscribe(
      data=>{
        if(data.status == 200){
          this.getHostNameAndIPMappings(this.pageSize,this.pageNumber,this.searchIP)
        }
      }
    )
  }
  onAdd(){
    this.AddHostNameAndIPMapping = true;
    this.hostNameAndIPMapping = new HostNameAndIpmappingModule();
    setTimeout(() => {
      const input = document.querySelector('#hostname');
      const input$ = fromEvent(input, 'input');
      input$.pipe(
        debounceTime(700),
        distinctUntilChanged(),
        switchMap(
          e => this.searchAssetName(this.hostNameAndIPMapping.assetname)
        )
      ).subscribe(
        (response)=>{
          this.searchAssetNameloading = false;
          if(response != null){
            this.assetNames = response.json();
          }
        }
      )   
      }, 100);
  }
  onEdit(){
    this.editHostNameAndIPMapping = true;
    this.hostNameAndIPMapping = this.selectedHostNameAndIPMappings[0];
    this.selectedAssetName = this.hostNameAndIPMapping.assetname;
    setTimeout(() => {
      const input = document.querySelector('#hostnameedit');
      const input$ = fromEvent(input, 'input');
      input$.pipe(
        debounceTime(700),
        distinctUntilChanged(),
        switchMap(
          e => this.searchAssetName(this.hostNameAndIPMapping.assetname)
        )
      ).subscribe(
        (response)=>{
          this.searchAssetNameloading = false;
          if(response != null){
            this.assetNames = response.json();
          }
        }
      )   
      }, 100);
  }
  selectItem(item:any){
    this.hostNameAndIPMapping.assetname = item;
    this.selectedAssetName = item;
  }
  hidden:boolean = true;
  focus(){
    this.hidden = false;
    this.hostNameAndIPMapping.assetname = this.selectedAssetName;
    
  }
  blur(){
    setTimeout(() => {
    this.hidden = true;
    this.hostNameAndIPMapping.assetname = this.selectedAssetName;
    }, 150);
  }
  searchAssetNameloading:boolean = false;
  assetNames:string[] = [];
  searchAssetName(content:string){
    if(content == ""){
      return of(null);
    }else{
      this.searchAssetNameloading = true;
      return this.service.searchAssetNameList(content);
    }
  }
  cancelSave(){
    this.AddHostNameAndIPMapping = false;
    this.selectedAssetName = "";
    this.editHostNameAndIPMapping = false;
    this.assetNames = [];
  }
  saveMappingErrorShow:boolean = false;
  saveMappingError:string = "";
  saveMappingLoading:boolean = false;
  confirmSave(){
    this.saveMappingLoading = true;
    this.service.saveHostNameAndIPMapping(this.hostNameAndIPMapping).subscribe(
      (data)=>{
        if(data.status == 201){
          this.selectedAssetName = "";
          this.saveMappingLoading = false;
          this.AddHostNameAndIPMapping = false;
          this.assetNames = [];
          this.getHostNameAndIPMappings(this.pageSize,this.pageNumber,this.searchIP) 
        }
      },error=>{
        this.saveMappingLoading = false;
        this.saveMappingErrorShow = true;
        this.assetNames = [];
        this.saveMappingError = error.json().message;
      }
    )
  }

  confirmUpdate(){
    this.saveMappingLoading = true;
    this.service.updateHostNameAndIPMapping(this.hostNameAndIPMapping).subscribe(
      (data)=>{
        if(data.status == 200){
          this.selectedAssetName = "";
          this.saveMappingLoading = false;
          this.editHostNameAndIPMapping = false;
          this.assetNames = [];
          this.getHostNameAndIPMappings(this.pageSize,this.pageNumber,this.searchIP)
        }
      },error=>{
        this.saveMappingLoading = false;
        this.saveMappingErrorShow = true;
        this.assetNames = [];
        this.saveMappingError = error.json().message;
      }
    )
  }
  invalidIP = false;
  ipRegx:string =  "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."

  +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
  
  +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
  
  +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
  getIPValidationState(){
    return this.invalidIP;
  }
  handleIPValidation(flag: boolean): void {
    if(flag){
      if(this.hostNameAndIPMapping.ip != null && this.hostNameAndIPMapping.ip !='' 
        && this.hostNameAndIPMapping.ip.match(this.ipRegx)){
        this.invalidIP = false;
      }else{
        this.invalidIP = true;
      }
    }
  }

  private API_URL = environment.API_URL;
  
  uploadMappingFile:boolean = false;
  uploadBtnLoading:boolean = false;
  uploadError:boolean = false;
  uploadErrorMsg:string = "";
  failureMappings:HostNameAndIpmappingModule[] = [];
  failureMappingsShow:boolean = false;
  uploader:FileUploader = new FileUploader({
    url: ""+this.API_URL+"/v1/assets/mapping/hostnameip/file",
    method: "POST", 
    itemAlias: "file", 
    headers:[
      {name:"Authorization",value:'Bearer '+this.auth.getToken()}
    ]
  });

  openUpload(){
    this.uploadMappingFile = true;
    this.uploadError = false;
  }

  selectMappingFileOnChanged(event: any) {
    let eventNameArr:string[] = event.target.value.split(".");
    let upload = <HTMLInputElement>document.getElementById("selectMappingFile");
    if(eventNameArr[eventNameArr.length-1].toLocaleLowerCase() != "txt"){
      this.searchErrorShow = true;
      this.searchErrorMsg = "Please select an txt file";
      this.uploader.clearQueue();
      if(upload != null){
        upload.value = null;
      }
    }else{
      this.uploadError = false;
    }
  }

  cancelUpload(){
    this.uploadMappingFile = false;
    this.cleanUploadQueue();
    this.failureMappingsShow = false;
    this.failureMappings = [];
  }

  uploadFile() {
    this.failureMappingsShow = false;
    this.failureMappings = [];
    let upload = <HTMLInputElement>document.querySelector("#selectMappingFile");
    if(upload.value == null || upload.value == ""){
      this.uploadError = true;
      this.uploadErrorMsg = "Please select a file first";
    }else{
      this.uploader.queue[0].upload();
      this.loading = true;
      this.uploadBtnLoading = true;
    }
  }

  successItem(item: FileItem, response: string, status: number, headers: ParsedResponseHeaders){
    this.loading = false;
    this.uploadBtnLoading = false;
    this.cleanUploadQueue();
    if (status != 200) {
      this.uploadError = true;
      this.uploadErrorMsg = "Upload failure" + response;
    }else{
      this.pageSize = 10;
      this.pageNumber = 1;
      this.searchIP = null;
      this.changePageSize();
      this.failureMappings = JSON.parse(response);
      if(this.failureMappings.length == 0){
        this.uploadMappingFile = false;
        this.failureMappingsShow = false;
      }else{
        this.failureMappingsShow = true;
      }
    }
  }
  errorItem(item: FileItem, response: string, status: number, headers: ParsedResponseHeaders){
    this.cleanUploadQueue();
    this.loading = false;
    this.uploadBtnLoading = false;
    this.uploadError = true;
    this.uploadErrorMsg = "Upload failure";
  }

  cleanUploadQueue(){
    this.uploader.clearQueue();
    let upload = <HTMLInputElement>document.querySelector("#selectMappingFile");
    upload.value = "";
  }
  ngOnInit() {
    this.getFirstPageData();
   
  }

}