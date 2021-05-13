/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {map} from 'rxjs/operators';
import { Component, OnInit } from '@angular/core';
import { ServermappingService } from './servermapping.service';
import {Router,ActivatedRoute} from '@angular/router';
import { AssetModule } from './asset.module';
import { forkJoin ,  SubscribableOrPromise } from 'rxjs';
import { SddcsoftwareModule } from '../sddcsoftware/sddcsoftware.module';
import { ServerMappingDataModule } from './mapping.module';
import { ClrDatagridStateInterface } from '@clr/angular';


@Component({
  selector: 'app-servermapping',
  templateUrl: './servermapping.component.html',
  styleUrls: ['./servermapping.component.scss']
})

export class ServermappingComponent implements OnInit {

  constructor(private service:ServermappingService,private router: Router, private route: ActivatedRoute) {  
    this.selectedOtherAssets = [];
    this.serverconfig.type = "";
    this.serverconfig.id = ""; 
  }
 xah_obj_to_map = ( obj => {
    const mp = new Map;
    Object.keys ( obj ). forEach (k => { mp.set(k, obj[k]) });
    return mp;
  });
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

  serverconfigs:SddcsoftwareModule[] = [];
  serverconfig:SddcsoftwareModule= new SddcsoftwareModule();
 
  serverMappings:ServerMappingDataModule[] = [];
  serverMapping:ServerMappingDataModule = new ServerMappingDataModule();

  setInfo(){
    this.info=this.pageSize;
    this.refresh(this.currentState);
  }
  changeType(){
    if(this.serverconfig.type == "VRO" || this.serverconfig.type == "VROPSMP"){
      this.vrohid = false;
      this.hid = true;
    }else if(this.serverconfig.type == "VCENTER"){
      this.vrohid = true;
      this.hid = false;
    }
    this.getServerConfigs();
  }
  change(){
      this.refresh(this.currentState);
  }
  getServerConfigs(){
    this.service.getserverconfigs(this.serverconfig.type).subscribe(
      (data:SddcsoftwareModule[])=>{
        this.serverconfigs = data;
      }
    )
    this.serverMappings = [];
  }
  currentState:ClrDatagridStateInterface;
  totalItems:number = 0;
  serverMappingLoading:boolean = false;
  refresh(state: ClrDatagridStateInterface){
    this.serverMappings = [];
    if (!state.page) {
      return;
    }
    this.currentState = state;
    this.getServerMappings(state.page.current,state.page.size);
  }
  getServerMappings(currentPage:number,pageSize:number){
    if(this.serverconfig.id !=""){
      this.serverMappingLoading = true;
      this.service.getServerMappings(pageSize,currentPage,this.serverconfig.type,this.serverconfig.id).subscribe(
        (data)=>{
          this.nulltips = "No mapping found!";
          this.serverMappings = [];
          this.serverMappings = data['content'];
          this.totalItems = data['totalElements'];
          this.serverMappingLoading = false;
      },(error)=>{
        this.serverMappingLoading = false;
      })
    }else{
      this.nulltips = "Please select a server first.";
      this.serverMappings = [];
    }
  }
  updateServerMapping(id:string,assetID:string){
    this.service.updateServerMapping(id,assetID).subscribe(
      (data)=>{
        this.mappedServerAsset.id = "";
        this.mappedServerModalOpen = false;
        this.refresh(this.currentState);
    },(error)=>{
        alert(error.json().errors[0]);
    })
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.refresh(this.currentState);
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++;
      this.refresh(this.currentState);
    }
  }

  //*****************Asset************************* */
  totalPageAsset:number=1;
  currentPageAsset:number=1;
  pageSizeAsset:number=5;
  serverMappingId:string="";
  disabledAsset:string="disabled";
  keywords:string="";
  mappedServerAsset:AssetModule = new AssetModule();
  assets :AssetModule[] = [];
  asset:AssetModule=new AssetModule();
  mappedOtherAssets:AssetModule[] = [];
  selectedOtherAssets:AssetModule[];
  category:string = "Server";
  showOtherCategory:string="";
  searchBtnState:boolean = false;
  searchBtnDisabled:boolean = false;
  // assetListCurrentState:ClrDatagridStateInterface;
  selectedServerAsset:AssetModule = new AssetModule();
  showServerAsset(mappingId:string){
    this.currentPageAsset = 1;
    this.service.getMappingById(mappingId).subscribe(
      (data:ServerMappingDataModule)=>{
        this.serverMapping = data;
        if(this.serverMapping.asset != null){
          this.getAssetById(this.serverMapping.asset);
        }else{
          this.mappedServerAsset = new AssetModule();
        }
      }
    )
    this.category = "Server";
    this.currentPage = 1;
    this.keywords="";
    this.mappedServerModalOpen = true;
  }
  getAssetById(id:string):AssetModule{
    let asset:AssetModule = new AssetModule();
    this.service.getAssetById(id).subscribe(
      (data:AssetModule)=>{
        this.mappedServerAsset= data;
        this.mappedServerAsset.enable = true;
        this.selectedServerAsset = this.mappedServerAsset;
      }
    )
    return asset;
  }
  updateAssetModalErrorShow:boolean = false;
  updateAssetError:string = "Internal error";
  
  showOtherAssetMapping(id:string,category:string){
    this.keywords = "";
    this.mappedOtherAssetModalOpen = true;
    this.assets = [];
    this.category = category;
    this.currentPageAsset = 1;
    this.mappedOtherAssets = [];
    if(category == "PDU"){
      this.showOtherCategory = "PDUs";
    }else if(category == "Networks"){
      this.showOtherCategory = "Switches";
    }
    this.service.getMappingById(id).subscribe(
        (data:ServerMappingDataModule)=>{
            this.serverMapping = data;
            let assetId = this.serverMapping.asset;
            this.service.getAssetById(assetId).subscribe(
              (data:AssetModule)=>{
                this.mappedServerAsset= data;
                let reqList:SubscribableOrPromise<any>[] = [];
                let assetIDs:string[] = [];
                if(category == "PDU"){
                  assetIDs = this.mappedServerAsset.pdus;
                }else if(category == "Networks"){
                  //switch
                  assetIDs = this.mappedServerAsset.switches;
                }
                if(assetIDs != null){
                  for(let i=0;i<assetIDs.length; i++){
                    reqList.push(this.service.getAssetById(assetIDs[i]));
                  }
                }
                
                forkJoin(reqList).pipe(map((data)=>data)).subscribe((res)=>{
                  res.forEach((element:AssetModule) => {
                    element.enable = true;
                    this.mappedOtherAssets.push(element)
                    
                  });
                })
                this.currentPage = 1;
                this.keywords="";
             
              },error=>{
                this.updateAssetError = error.json().message();
                this.updateAssetModalErrorShow = true;
              }
            )   
          
        },error=>{
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
  emptyResult:boolean = false;
  notEnoughOnepage:boolean = false;
  getAssets(){
    this.searchBtnDisabled = true;
    this.searchBtnState = true;
    this.loading = true;
      this.service.getAssets(this.pageSizeAsset,this.currentPageAsset,this.keywords,this.category).subscribe(
        (data)=>{
          this.searchBtnDisabled = false;
          this.searchBtnState = false;
          this.loading = false;
          this.assets = data['content'];
          this.assets.forEach(element => {
            element.enable = true;
          });
          if(data['content'].length == this.pageSizeAsset){
            this.emptyResult = false;
            this.currentPageAsset = data['number']+1;
            this.disabledAsset = "";
          }else{
            this.emptyResult = true;
            this.disabledAsset = "disabled";
          }
      },(error)=>{
        this.updateAssetModalErrorShow = true;
        this.updateAssetError = error.json().message
        this.loading = false;
        this.searchBtnDisabled = false;
        this.searchBtnState = false;
      })
    
  }

  previousAsset(){
    if(this.currentPageAsset>1){
      this.currentPageAsset--;
      this.getAssets();
    }
  }
  nextAsset(){
    if(!this.emptyResult){
      this.currentPageAsset++;
    }
    this.getAssets();
  }
    

  closeSelectOtherAsset(){
    this.mappedOtherAssets = [];
    this.mappedOtherAssetModalOpen = false;
    this.updateAssetModalErrorShow = false;
    this.updateAssetError = "Internal error";
    this.keywords = "";
  }

  //map sensor
  metricsDatas:any[]=[];
  mappedSensorAssetModalOpen:boolean = false; 
  fronTemIds:string[] = [];
  backTempIds:string[] = [];
  frontHumIds:string[] = [];
  backHumIds:string[] = [];
  mappingId:string = "";
  showSensors(id:string, category:string,fillData){
    this.fronTemIds = [];
    this.backTempIds = [];
    this.backHumIds = [];
    this.frontHumIds = [];
    this.mappedSensorAssetModalOpen = true;
    this.category = category;
    this.mappingId = id;
    this.service.getMappingById(id).subscribe(
      (data:ServerMappingDataModule)=>{
        this.serverMapping = data;
        let assetId = this.serverMapping.asset;
        this.service.getAssetById(assetId).subscribe(
          (data:AssetModule)=>{
              this.mappedServerAsset= data;
              let sensorformular = this.xah_obj_to_map(this.mappedServerAsset.metricsformulars).get('SENSOR');
              if(sensorformular != null){
                let sensorMap = this.xah_obj_to_map(JSON.parse(sensorformular));
                if(sensorMap.has("FrontTemperature")){
                  let frontTemMap = this.xah_obj_to_map(sensorMap.get("FrontTemperature"));
                  this.fronTemIds = [];
                  frontTemMap.forEach((value: string, key: any)  => {
                    this.fronTemIds.push(value);
                  });
                  this.getSensors("Front Temperature",this.fronTemIds);
                }
                if(sensorMap.has("BackTemperature")){
                  let backTempMap = this.xah_obj_to_map(sensorMap.get("BackTemperature"));
                  this.backTempIds = [];
                  backTempMap.forEach((value: string, key: any)  => {
                    this.backTempIds.push(value);
                  });
                  this.getSensors("Back Temperature",this.backTempIds);
                }
                if(sensorMap.has("FrontHumidity")){
                  let frontHumMap = this.xah_obj_to_map(sensorMap.get("FrontHumidity"));
                  this.frontHumIds = [];
                  frontHumMap.forEach((value: string, key: any)  => {
                    this.frontHumIds.push(value);
                  });
                  this.getSensors("Front Humidity",this.frontHumIds);
                }
                if(sensorMap.has("BackHumidity")){
                  let backHumMap = this.xah_obj_to_map(sensorMap.get("BackHumidity"));
                  this.backHumIds = [];
                  backHumMap.forEach((value: string, key: any)  => {
                    this.backHumIds.push(value);
                  });
                  this.getSensors("Back Humidity",this.backHumIds);
                }
              }
              this.fillData();
          },error=>{
              this.updateAssetError = error.json().message();
              this.updateAssetModalErrorShow = true;
            }
          )   
      },error=>{
        this.updateAssetError = error.json().message();
        this.updateAssetModalErrorShow = true;
      }
    )
  }
  frontTemSensors:AssetModule[] = [];
  backTemSensors:AssetModule[] = [];
  frontHumSensors:AssetModule[] = [];
  backHumSensors:AssetModule[] = [];
  
  getSensors(metricName:string, ids:string[]){
    this.currentPageAsset = 1;
    let reqList:SubscribableOrPromise<any>[] = [];
    for(let i=0;i<ids.length; i++){
      reqList.push(this.service.getAssetById(ids[i]));
    }
     forkJoin(reqList).pipe(map((data)=>data)).subscribe((res)=>{
      res.forEach((element:AssetModule) => {
        let sensorAsset:AssetModule = new AssetModule();
        sensorAsset = element;
        sensorAsset.enable = true;
        if(metricName == "Front Temperature"){
          this.frontTemSensors.push(sensorAsset);
        }else if(metricName == "Back Temperature"){
          this.backTemSensors.push(sensorAsset);
        }else if(metricName == "Front Humidity"){
          this.frontHumSensors.push(sensorAsset);
        }else if(metricName == "Back Humidity"){
          this.backHumSensors.push(sensorAsset);
        }
  
      });
    })
  }
  fillData(){
    this.metricsDatas = [];
    this.metricsDatas.push({"metricName":"Front Temperature","sensors":this.frontTemSensors});
    this.metricsDatas.push({"metricName":"Back Temperature","sensors":this.backTemSensors});
    this.metricsDatas.push({"metricName":"Front Humidity","sensors":this.frontHumSensors});
    this.metricsDatas.push({"metricName":"Back Humidity","sensors":this.backHumSensors});
  }
  closeMappingSensor(){
    this.mappedSensorAssetModalOpen = false;
    this.frontTemSensors = [];
    this.backTemSensors = [];
    this.frontHumSensors = [];
    this.backHumSensors = [];
    this.metricsDatas = [];
  }

  selectSensorShow:boolean = false;
  mappedSensors:AssetModule[] = [];
  metricName:string="";
  selectedSensors:AssetModule[] = []
  selectSensor(metricName:string){
    this.keywords = "";
    this.currentPageAsset = 1;
    this.metricName = metricName;
    this.selectSensorShow = true;
    this.assets = [];
    if(metricName == "Front Temperature"){
      this.mappedSensors = this.frontTemSensors;
    }else if(metricName == "Back Temperature"){
      this.mappedSensors =  this.backTemSensors;
    }else if(metricName == "Front Humidity"){
      this.mappedSensors = this.frontHumSensors;
    }else if(metricName == "Back Humidity"){
      this.mappedSensors =  this.backHumSensors;
    }
  }
  closeSelectSelectSensor(){
    this.loading = false;
    this.keywords = "";
    this.selectedSensors = [];
    this.selectSensorShow = false;
    this.frontTemSensors = [];
    this.backTemSensors = [];
    this.frontHumSensors = [];
    this.backHumSensors = [];
    this.assets = [];
    this.showSensors(this.mappingId,this.category,this.fillData);
    this.mappedSensors.forEach(element => {
      element.enable = true;
    });
  }
  confirmUpdateServerAsset(){
    this.loading = true;
    let update:boolean = false;
    let assetToUpdate:AssetModule = new AssetModule();
    assetToUpdate.id = this.mappedServerAsset.id;
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
      if(this.mappedServerAsset.pdus == null || this.mappedServerAsset.pdus.length == 0){
        update = true;
        assetToUpdate.pdus = pduids;
      }else{
        if(this.mappedServerAsset.pdus.length != pduids.length){
          update = true;
          assetToUpdate.pdus = pduids;
        }else{
          pduids.forEach(element => {
            if(!update){
              if(this.mappedServerAsset.pdus.indexOf(element) == -1){
                update = true;
                assetToUpdate.pdus = pduids;
              }
            }
          });
        }
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
      if(this.mappedServerAsset.switches == null || this.mappedServerAsset.switches.length == 0){
        update = true;
        assetToUpdate.switches = switchids;
      }else{
        if( this.mappedServerAsset.switches.length != switchids.length){
          update = true;
          assetToUpdate.switches = switchids;
        }else{
          switchids.forEach(element => {
            if(!update){
              if(this.mappedServerAsset.switches.indexOf(element) == -1){
                update = true;
                assetToUpdate.switches = switchids;
              }
            }
          });
        }
      }
      
    }else if(this.category == "Sensors"){
      let sensorids:string[] = [];
      this.assets = [];
      this.mappedSensors.forEach(element => {
        if(element.enable){
          sensorids.push(element.id);
        }
      }); 
      this.selectedSensors.forEach(element1 => {
        if(sensorids.indexOf(element1.id) == -1){
          sensorids.push(element1.id);
        }
      });
      let oldsensorId:string[] = [];
      let metricName:string = "";
      if(this.metricName == "Front Temperature"){
        metricName = "FrontTemperature";
        oldsensorId = this.fronTemIds;
      }else if(this.metricName == "Back Temperature"){
        metricName = "BackTemperature";
        oldsensorId = this.backTempIds;
      }else if(this.metricName == "Front Humidity"){
        metricName = "FrontHumidity";
        oldsensorId = this.frontHumIds;
      }else if(this.metricName == "Back Humidity"){
        metricName = "BackHumidity";
        oldsensorId = this.backHumIds;
      }
      if(sensorids.length != oldsensorId.length){
        update = true;
        let positionMap:Map<string,string> = new Map<string,string>();
        sensorids.forEach(element =>{
          positionMap.set(element,element);
        })
        assetToUpdate.metricsformulars = this.generateMetricFormula(positionMap,metricName);
      }else{
        sensorids.forEach(element => {
          let positionMap:Map<string,string> = new Map<string,string>();
          positionMap.set(element,element);
          if(!update){
            if(oldsensorId.indexOf(element) == -1){
              update = true;
              assetToUpdate.metricsformulars = this.generateMetricFormula(positionMap,metricName);
            }
          }
        });
      }
    }
    if(update){
      this.service.updateAsset(assetToUpdate).subscribe(data=>{
          this.loading = false;
          this.mappedOtherAssetModalOpen = false;
          if(this.category == "Sensors"){
            this.closeSelectSelectSensor();
            this.mappedOtherAssets = [];
          }
      },error=>{
        this.loading = false;
        this.updateAssetModalErrorShow = true;
        this.updateAssetError = error.json().message;
        this.mappedOtherAssets = [];
      })
    }else{
      this.loading = false;
      this.keywords = "";
      this.mappedOtherAssets = [];
      this.mappedOtherAssetModalOpen = false;
      if(this.category == 'Sensors'){
        this.selectSensorShow = false;
        this.mappedSensors = [];
        this.metricName="";
        this.selectedSensors = []
        this.mappedOtherAssets = [];
      }
    }
  
  }
  generateMetricFormula(positionMap:Map<string,string>,metricName:string):{}{
    let metricsFormulaMap:Map<string,{}> = new Map<string,{}>();
    let positinoMapjsonObject = {};  
    positionMap.forEach((value, key) => {  
      positinoMapjsonObject[key] = value  
    });  
    let sensorMap:Map<string,{}> = new Map<string,{}>();
    sensorMap.set(metricName,positinoMapjsonObject);
    let sensorMapjsonObject = this.convertToJsonObject(sensorMap);  
    metricsFormulaMap.set('SENSOR',JSON.stringify(sensorMapjsonObject));
    let metricMapjsonObject = this.convertToJsonObject(metricsFormulaMap);   
    return metricMapjsonObject;
  }
  convertToJsonObject(map:Map<string,{}>):{}{
    let jsonObject = {};  
    map.forEach((value, key) => {  
      jsonObject[key] = value  
        });  
    return jsonObject;
  }
  cancelAsset(){
    this.assets = [];
    this.mappedServerModalOpen = false;
  }
  confirmAsset(asset:AssetModule){
    this.assets = [];
    if(asset.enable){
      this.updateServerMapping(this.serverMapping.id,asset.id);
    }else{
      this.updateServerMapping(this.serverMapping.id,null);
    }
  }
  
  ngOnInit() {
  }

  cancel(){
    this.basic = false;
    this.serverMappingId = "";
  }
  confirm(){
    this.service.deleteServerMapping(this.serverMappingId).subscribe(
      data=>{
        this.basic = false;
        this.refresh(this.currentState);
    },error=>{
        this.basic = false;
    })
  }

}
