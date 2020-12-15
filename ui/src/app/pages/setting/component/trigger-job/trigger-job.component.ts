/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit, ViewChild } from '@angular/core';
import { SettingService } from '../../setting.service';
import { FormBuilder, FormGroup, NgForm, Validators } from '@angular/forms';
import { HostNameAndIpmappingModule } from '../../host-name-and-ipmapping/host-name-and-ipmapping.module';
import { fromEvent ,  of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { FileUploader, ParsedResponseHeaders, FileItem } from 'ng2-file-upload';
import {environment} from '../../../../../environments/environment.prod';
import { AuthenticationService } from '../../../auth/authenticationService';
import { ClrDatagridStateInterface } from '@clr/angular';
import { HttpErrorResponse } from '@angular/common/http';
import { SummaryModule } from '../../summary/summary.module';

@Component({
  selector: 'app-trigger-job',
  templateUrl: './trigger-job.component.html',
  styleUrls: ['./trigger-job.component.scss']
})
export class TriggerJobComponent implements OnInit {
  addAssetIPAndNameMappingForm:FormGroup;
  editAssetIPAndNameMappingForm:FormGroup;
  ipRegx:string =  "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."

  +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
  
  +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
  
  +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
  constructor(private service:SettingService, private auth:AuthenticationService,private fb: FormBuilder) { 
    this.uploader.onSuccessItem = this.successItem.bind(this);
    this.uploader.onErrorItem = this.errorItem.bind(this);
    this.addAssetIPAndNameMappingForm =  this.fb.group({
      ip: ['', [
        Validators.required,
        Validators.pattern(this.ipRegx)
      ]],
      assetname: ['', [
        Validators.required
      ]]
    });
    this.editAssetIPAndNameMappingForm =  this.fb.group({
      id: ['', [
      ]],
      ip: ['', [
        Validators.required,
        Validators.pattern(this.ipRegx)
      ]],
      assetname: ['', [
        Validators.required
      ]]
    });
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
  servers:string[]=[];
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
  unmappedServerErrorMsg:string = "";
  flowgateSummary:SummaryModule = new SummaryModule();
  
  close(){
    this.alertclose = true
  }
  trigger(jobname:string){

    if(jobname == "job1"){
      this.serverMapping = true;
      this.service.mergeserverMapping().subscribe(
        (data)=>{
          this.serverMapping = false;
          this.alertType = "success";
          this.alertcontent = "Trigger Success";
          this.alertclose = false;
          setTimeout(() => {
            this.alertclose = true  
          },2000);
        },error=>{
          this.serverMapping = false;
          this.alertType = "danger";
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
        
          this.pduMapping = false;
          this.alertType = "success";
          this.alertcontent = "Trigger Success";
          this.alertclose = false;
          setTimeout(() => {
            this.alertclose = true  
          },2000);

        },error=>{
          this.pduMapping = false;
          this.alertType = "danger";
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

          this.temphumidityMapping = false;
          this.alertType = "success";
          this.alertcontent = "Trigger Success";
          this.alertclose = false;
          setTimeout(() => {
            this.alertclose = true  
          },2000);
      
        },error=>{
          this.temphumidityMapping = false;
          this.alertType = "danger";
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
    this.unmappedServerErrorMsg = "";
  }
  save(){
    let tosaveTime = this.toUpdateExpiredTimeRange*24*3600*1000;
    this.service.updatesTimeRange(tosaveTime).subscribe(
      (data)=>{
        this.updateExpiredTime = false;
        this.errorShow = false;
        this.errorMsg = "";
        this.getExpiredTimeRange();
      },(error:HttpErrorResponse)=>{
        this.errorShow = true;
        this.errorMsg = error.message;
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
      (data:Number)=>{
        this.expiredTimeRange = data;
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
        (data:string[])=>{
          this.servers = data;
          this.serverloading = false;
          this.unmappedServerErrorMsg = "";
        },(error:HttpErrorResponse)=>{
          this.unmappedservershow = true;
          this.unmappedServerErrorMsg = error.message;
        }
      )
    },2000);

  }
  getFirstPageData(){
    this.service.getSystemSummaryData().subscribe(
      (data:SummaryModule)=>{
        this.flowgateSummary = data;
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
  loading:boolean = true;
  disabled:string="";
  selectedAssetName:string="";

  searchIP:string = null;
  searchBtnState:boolean = false;
  searchBtnDisabled:boolean = false;
  searchErrorShow:boolean = false;
  searchErrorMsg:string = "";
  search(){
    if(this.searchIP != null){
      let ip = this.searchIP.trim();
      if(ip == ""){
        this.searchIP = null;
        this.refresh(this.currentState);
      }else{
        if(ip.match(this.ipRegx)){
          this.pageSize = 10;
          this.pageNumber = 1;
          this.searchErrorShow = false;
          this.refresh(this.currentState);
        }else{
          this.searchErrorShow = true;
          this.searchErrorMsg = "Please enter a valid IP";
        }
      }
    }
  }
  currentState:ClrDatagridStateInterface;
  totalItems:number = 0;
  refresh(state: ClrDatagridStateInterface){
    if (!state.page) {
      return;
    }
    this.currentState = state;
    this.loading = true;
    this.getHostNameAndIPMappings(state.page.size,state.page.current);
  }

  getHostNameAndIPMappings(pagesize:number,pagenumber:number){
    this.service.getHostNameAndIPMapping(pagenumber,pagesize,this.searchIP).subscribe(
      data=>{
        this.selectedHostNameAndIPMappings = [];
        this.hostNameAndIPMappings = [];
        this.loading = false;
        this.hostNameAndIPMappings = data['content'];
        this.totalItems = data['totalElements'];
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
        this.selectedHostNameAndIPMappings = [];
        this.refresh(this.currentState);
      }
    )
  }
  onAdd(){
    this.AddHostNameAndIPMapping = true;
    this.saveMappingErrorShow = false;
    this.hostNameAndIPMapping = new HostNameAndIpmappingModule();
    setTimeout(() => {
      const input = document.querySelector('#hostname');
      const input$ = fromEvent(input, 'input');
      input$.pipe(
        debounceTime(700),
        distinctUntilChanged(),
        switchMap(
          e => this.searchAssetName(this.addAssetIPAndNameMappingForm.get('assetname').value)
        )
      ).subscribe(
        (response:string[])=>{
          this.searchAssetNameloading = false;
          if(response != null){
            this.assetNames = response;
          }
        }
      )   
      }, 100);
  }
  onEdit(){
    this.editHostNameAndIPMapping = true;
    this.saveMappingErrorShow = false;
    this.editAssetIPAndNameMappingForm.get("id").setValue(this.selectedHostNameAndIPMappings[0].id);
    this.editAssetIPAndNameMappingForm.get("ip").setValue(this.selectedHostNameAndIPMappings[0].ip);
    this.editAssetIPAndNameMappingForm.get("assetname").setValue(this.selectedHostNameAndIPMappings[0].assetname);
    this.selectedAssetName = this.selectedHostNameAndIPMappings[0].assetname;
    setTimeout(() => {
      const input = document.querySelector('#hostnameedit');
      const input$ = fromEvent(input, 'input');
      input$.pipe(
        debounceTime(700),
        distinctUntilChanged(),
        switchMap(
          e => this.searchAssetName(this.editAssetIPAndNameMappingForm.get('assetname').value)
        )
      ).subscribe(
        (response:string[])=>{
          this.searchAssetNameloading = false;
          if(response != null){
            this.assetNames = response;
          }
        }
      )   
      }, 100);
  }
  selectItem(item:any){
    this.selectedAssetName = item;
  }
  selectItemForEdit(item:any){
    this.selectedAssetName = item;
  }
  hidden:boolean = true;
  focusAdd(){
    this.hidden = false;
  }
  focusEdit(){
    this.hidden = false;
  }
  blurAdd(){
    setTimeout(() => {
    this.hidden = true;
    this.addAssetIPAndNameMappingForm.get("assetname").setValue(this.selectedAssetName);
    }, 150);
  }
  blurEdit(){
    setTimeout(() => {
    this.hidden = true;
    this.editAssetIPAndNameMappingForm.get("assetname").setValue(this.selectedAssetName);
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
    this.addAssetIPAndNameMappingForm.reset();
    this.editAssetIPAndNameMappingForm.reset();
    this.assetNames = [];
  }
  saveMappingErrorShow:boolean = false;
  saveMappingError:string = "";
  saveMappingLoading:boolean = false;
  confirmSave(){
    this.saveMappingLoading = true;
    let hostNameAndIPMapping:HostNameAndIpmappingModule = this.addAssetIPAndNameMappingForm.value;
    this.service.saveHostNameAndIPMapping(hostNameAndIPMapping).subscribe(
      (data)=>{
        this.selectedAssetName = "";
        this.saveMappingLoading = false;
        this.AddHostNameAndIPMapping = false;
        this.assetNames = [];
        this.addAssetIPAndNameMappingForm.reset();
        this.refresh(this.currentState);
      },(error:HttpErrorResponse)=>{
        this.saveMappingLoading = false;
        this.saveMappingErrorShow = true;
        this.assetNames = [];
        this.saveMappingError = error.error.message;
      }
    )
  }

  confirmUpdate(){
    this.saveMappingLoading = true;
    let hostNameAndIPMapping:HostNameAndIpmappingModule = this.editAssetIPAndNameMappingForm.value;
    this.service.updateHostNameAndIPMapping(hostNameAndIPMapping).subscribe(
      (data)=>{
        this.selectedAssetName = "";
        this.saveMappingLoading = false;
        this.editHostNameAndIPMapping = false;
        this.selectedHostNameAndIPMappings = [];
        this.assetNames = [];
        this.refresh(this.currentState);
      },(error:HttpErrorResponse)=>{
        this.saveMappingLoading = false;
        this.saveMappingErrorShow = true;
        this.assetNames = [];
        this.selectedHostNameAndIPMappings = [];
        this.saveMappingError = error.error.message;
      }
    )
  }
  invalidIP = false;
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
      this.refresh(this.currentState);
      this.searchIP = null;
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