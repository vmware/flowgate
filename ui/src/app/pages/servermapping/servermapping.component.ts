/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { ServermappingService } from './servermapping.service';
import {Router,ActivatedRoute} from '@angular/router';
import { AssetModule } from './asset.module';
import { forkJoin } from 'rxjs/observable/forkJoin';
import { Observable } from 'rxjs';
import { SubscribableOrPromise } from 'rxjs/Observable';

@Component({
  selector: 'app-servermapping',
  templateUrl: './servermapping.component.html',
  styleUrls: ['./servermapping.component.scss']
})
export class ServermappingComponent implements OnInit {

  constructor(private service:ServermappingService,private router: Router, private route: ActivatedRoute) {  
    this.selectedOtherAssets = [];
  }
  modal="";
  basic=false;
  clrAlertClosed:boolean = true;
  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '5';
  info:string='';
  mappedServerModalOpen:boolean=false;
  mappedOtherAssetModalOpen:boolean = false;
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
        this.mappedServerAsset.id = "";
        this.mappedServerModalOpen = false;
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
  mappedServerAsset:AssetModule = new AssetModule();
  assets :AssetModule[] = [];
  asset:AssetModule=new AssetModule();
  mappedOtherAssets:AssetModule[] = [];
  selectedOtherAssets:AssetModule[];
  category:string = "Server";
  showOtherCategory:string="";

  updateAssetID(mappingId:string){
    this.service.getMappingById(mappingId).subscribe(
      data=>{
        if(data.status == 200){
          this.serverMapping = data.json();
          if(this.serverMapping.asset != null){
            this.getAssetById(this.serverMapping.asset);
          }else{
            this.mappedServerAsset = new AssetModule();
          }
        }
      }
    )
    this.category = "Server";
    this.currentPage = 1;
    this.keywords="";
    this.mappedServerModalOpen = true;
    this.getAssets();
  }
  getAssetById(id:string):AssetModule{
    let asset:AssetModule = new AssetModule();
    this.service.getAssetById(id).subscribe(
      data=>{
        if(data.status == 200){
          this.mappedServerAsset= data.json();
        }
      }
    )
    return asset;
  }
  updateAssetModalErrorShow:boolean = false;
  updateAssetError:string = "";
  showOtherAssetMapping(id:string,category:string){
    this.mappedOtherAssetModalOpen = true;
    this.loading = true;
    this.category = category;
    if(category == "PDU"){
      this.showOtherCategory = "PDUs";
    }else if(category == "Networks"){
      this.showOtherCategory = "Switches";
    }
      this.service.getMappingById(id).subscribe(
        data=>{
          if(data.status == 200 && data.text() != ""){
            this.serverMapping = data.json();
            let assetId = this.serverMapping.asset;
            this.service.getAssetById(assetId).subscribe(
              data=>{
                if(data.status == 200){
                  this.mappedServerAsset= data.json();
                  let reqList:SubscribableOrPromise<any>[] = [];
                  let assetIDs:string[] = [];
                  if(category == "PDU"){
                    assetIDs = this.mappedServerAsset.pdus;
                  }else if(category == "Sensors"){
                    //sensor
                  }else if(category == "Networks"){
                    //switch
                    assetIDs = this.mappedServerAsset.switches;
                  }
                  for(let i=0;i<assetIDs.length; i++){
                    reqList.push(this.service.getAssetById(assetIDs[i]));
                  }
                  forkJoin(reqList).map((data)=>data).subscribe((res)=>{
                    res.forEach(element => {
                      if(element._body != null && element._body != ""){
                        let pduAsset:AssetModule = new AssetModule();
                        pduAsset = element.json();
                        pduAsset.enable = true;
                        this.mappedOtherAssets.push(pduAsset)
                      }
                    });
                  })
                  this.currentPage = 1;
                  this.keywords="";
                  this.getAssets();
                }
              },error=>{
                this.loading = false;
                this.updateAssetError = error.json().message();
                this.updateAssetModalErrorShow = true;
              }
            )   
          }else{
            this.loading = false;
            this.updateAssetError = "Not found server mapping";
            this.updateAssetModalErrorShow = true;
          }
        },error=>{
          this.loading = false;
          this.updateAssetError = error.json().message();
          this.updateAssetModalErrorShow = true;
        }
      )
  }
  deleteServerMapping(id){
    this.serverMappingId = id;
    this.basic = true;
  }
  search(){
    this.pageSizeAsset = 5;
    this.currentPageAsset = 1;
    this.getAssets();
  }
  validateMapped(mapping:any):boolean{
    return mapping.asset == null;
  }
  loading:boolean = false;
  getAssets(){
    this.loading = true;
    this.service.getAssets(this.pageSizeAsset,this.currentPageAsset,this.keywords,this.category).subscribe(data=>{
      if(data.status == 200){
        this.loading = false;
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
      this.loading = false;
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
  closeSelectOtherAsset(){
    this.mappedOtherAssets = [];
    this.mappedOtherAssetModalOpen = false;
    this.updateAssetModalErrorShow = false;
    this.updateAssetError = "";
  }
 
  confirmUpdateServerAsset(){
    this.loading = true;
    let update:boolean = false;
    if(this.category == "PDU"){
      let pduids:string[] = [];
      this.mappedOtherAssets.forEach(element => {
        if(element.enable){
          pduids.push(element.id);
        }
      }); 
      this.selectedOtherAssets.forEach(element1 => {
        if(pduids.indexOf(element1.id) == -1){
          pduids.push(element1.id);
        }
      });
      if(this.mappedServerAsset.pdus.length != pduids.length){
        update = true;
        this.mappedServerAsset.pdus = pduids;
      }else{
        pduids.forEach(element => {
          if(!update){
            if(this.mappedServerAsset.pdus.indexOf(element) == -1){
              update = true;
              this.mappedServerAsset.pdus = pduids;
            }
          }
        });
      }
    }else if(this.category == "Networks"){
      let switchids:string[] = [];
      this.mappedOtherAssets.forEach(element => {
        if(element.enable){
          switchids.push(element.id);
        }
      }); 
      this.selectedOtherAssets.forEach(element1 => {
        if(switchids.indexOf(element1.id) == -1){
          switchids.push(element1.id);
        }
      });
      if(this.mappedServerAsset.switches.length != switchids.length){
        update = true;
        this.mappedServerAsset.switches = switchids;
      }else{
        switchids.forEach(element => {
          if(!update){
            if(this.mappedServerAsset.switches.indexOf(element) == -1){
              update = true;
              this.mappedServerAsset.switches = switchids;
            }
          }
        });
      }
    }
    if(update){
      this.service.updateAsset(this.mappedServerAsset).subscribe(data=>{
        if(data.status == 200){
          this.loading = false;
          this.mappedOtherAssetModalOpen = false;
          this.mappedOtherAssets = [];
        }
      },error=>{
        this.loading = false;
        this.updateAssetModalErrorShow = true;
        this.updateAssetError = error.json().message;
      })
    }else{
      this.loading = false;
      this.mappedOtherAssets = [];
      this.mappedOtherAssetModalOpen = false;
    }
  
  }
  cancelAsset(){
    this.mappedServerModalOpen = false;
  }
  confirmAsset(asset:AssetModule){
    if(asset.id!=""){
      this.updateServerMapping(this.serverMapping.id,asset.id);
    }else{
      this.mappedServerModalOpen = false;
    }
  }

  ngOnInit() {
    this.getServerConfigs();
  }

  cancel(){
    this.basic = false;
    this.serverMappingId = "";
  }
  confirm(){
    this.service.deleteServerMapping(this.serverMappingId).subscribe(data=>{
      if(data.status == 200){
        this.basic = false;
        this.getServerMappings();
      }else{
        this.basic = false;
      }
    },
    error=>{
      this.basic = false;
    })
  }

}
