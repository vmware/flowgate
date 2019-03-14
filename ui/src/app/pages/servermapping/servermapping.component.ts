/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { ServermappingService } from './servermapping.service';
import {Router,ActivatedRoute} from '@angular/router';
import { error } from 'util';
import {ClrDatagridFilterInterface} from "@clr/angular";

@Component({
  selector: 'app-servermapping',
  templateUrl: './servermapping.component.html',
  styleUrls: ['./servermapping.component.scss']
})
export class ServermappingComponent implements OnInit {

  constructor(private service:ServermappingService,private router: Router, private route: ActivatedRoute) { }
  modal="";
  basic=false;

  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '5';
  info:string='';
  opend:boolean=false;
  vrohid:boolean=false;
  hid:boolean=true;
  disabled:String="";
  nulltips="Please select a server first.";
  serverconfigs=[{
    id:"id",
    name:"name"
  }];
  serverconfig={
    id:"",
    name:"",
    type:"VRO"
  }
 
  serverMappings=[];
  serverMapping={
    id:"",
    vroResourceName:"",
    vroVMEntityName:"",
    vroVMEntityObjectID:"",
    vroVMEntityVCID:"",
    vroResourceID:"",
    asset:""
  }
  serverMappingVCs=[];
  serverMappingVC={
    vcHostName:"",
    vcMobID:"",
    asset:""
  }
  setInfo(){
    this.info=this.pageSize;
   
    this.getServerMappings();
  }
  changeType(){
    if(this.serverconfig.type == "VRO"){
      this.vrohid = false;
      this.hid = true;
    }else if(this.serverconfig.type == "VCENTER"){
      this.vrohid = true;
      this.hid = false;
    }
    this.getServerConfigs();
  }
  change(){
      this.getServerMappings();
  }
  getServerConfigs(){
    this.service.getserverconfigs(this.serverconfig.type).subscribe(
      (data)=>{
        if(data.status == 200){
          this.serverconfigs = data.json();
        }
      }
    )
    this.serverMappings = [];
  }
  getServerMappings(){
    if(this.serverconfig.id !=""){
      this.service.getServerMappings(this.pageSize,this.currentPage,this.serverconfig.type,this.serverconfig.id).subscribe((data)=>{
        this.nulltips = "No mapping found!";
        this.serverMappings = [];
        if(data.status == 200){
               this.serverMappings = data.json().content
               this.currentPage = data.json().number+1;
               this.totalPage = data.json().totalPages;
              if(this.totalPage == 1){
                this.disabled = "disabled";
              }else{
                this.disabled = "";
              }
        }else{
          this.serverMappings = [];
        }
      })
    }else{
      this.nulltips = "Please select a server first";
      this.serverMappings = [];
    }
  }
  updateServerMapping(id,assetID){
    this.service.updateServerMapping(id,assetID).subscribe((data)=>{
      if(data.status == 200){
        this.selectedAsset.id = "";
        this.opend = false;
        this.getServerMappings();
      }
    },
    (error)=>{
      alert(error.json().errors[0]);
    })
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.getServerMappings();
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++;
      this.getServerMappings();
    }
  }

  //*****************Asset************************* */
  totalPageAsset:number=1;
  currentPageAsset:number=1;
  pageSizeAsset:number=5;
  serverMappingId:string="";
  disabledAsset:string="";
  keywords:string="";
  selectedAsset={
    id:"",
    assetName:"",
    assetSource:"",
    manufacturer:"",
    model:""
  };
  assets = []
  asset={
    id:"",
    assetName:"",
    assetSource:"",
    manufacturer:"",
    model:""
  }
 
  updateAssetID(id){
    this.serverMappingId = id;
    this.opend = true;
    this.getAssets();
  }
  search(){
    this.pageSizeAsset = 5;
    this.currentPageAsset = 1;
    this.getAssets();
  }
  getAssets(){
    this.service.getAssets(this.pageSizeAsset,this.currentPageAsset,this.keywords).subscribe(data=>{
      if(data.status == 200){
        this.assets = data.json().content;
        this.currentPageAsset = data.json().number+1;
        this.totalPageAsset = data.json().totalPages;
        if(this.totalPageAsset == 1){
          this.disabledAsset = "disabled";
        }else{
          this.disabledAsset = "";
        }
      }
    },
    (error)=>{
      alert(error.json().errors[0]);
    })
  }
  previousAsset(){
    if(this.currentPageAsset>1){
      this.currentPageAsset--;
      this.getAssets();
    }
  }
  nextAsset(){
    if(this.currentPageAsset < this.totalPageAsset){
      this.currentPageAsset++;
      this.getAssets();
    }
  }
  cancelAsset(){
  
    this.selectedAsset.id = "";
    this.opend = false;
  }
  confirmAsset(id){

    if(id!=""){
      this.updateServerMapping(this.serverMappingId,id);
    }else{
      this.opend = false;
    }
   
  }

  ngOnInit() {
    this.getServerConfigs();
  }

}
