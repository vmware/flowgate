/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {map} from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { AuthenticationService } from '../auth/authenticationService';
import {environment} from 'environments/environment.prod';
import { HostNameAndIpmappingModule } from './host-name-and-ipmapping/host-name-and-ipmapping.module';
import { AssetModule } from './component/asset-modules/asset.module';
import { HttpClient,HttpHeaders } from '@angular/common/http';

const options = {
  headers:new HttpHeaders().set('Content-Type','application/json')
}

@Injectable()
export class SettingService {

  private API_URL = environment.API_URL;

  constructor(private http:HttpClient) { 
  }
  postsensorsetting(type,minNum,maxNum,minValue,maxValue){
  let body = JSON.stringify({
    type:type,
    minNum:minNum,
    maxNum:maxNum,
    minValue:minValue,
    maxValue:maxValue
  });
  
  return this.http.post(""+this.API_URL+"/v1/sensors/setting", body,options).pipe(map((res)=>res))
  }

  getsensorsetting(id){
    return this.http.get(""+this.API_URL+"/v1/sensors/setting/"+id+"",options).pipe(
      map((res)=>res))
  }
  
  createAnAsset(asset:AssetModule){
    let body = JSON.stringify(asset);
    return this.http.post(""+this.API_URL+"/v1/assets", body,options).pipe(map((res)=>res))
  }

  getAssetsBySource(source, currentPage, pageSize){
    return this.http.get(""+this.API_URL+"/v1/assets/source/"+source+"?currentPage="+currentPage+"&pageSize="+pageSize, options).pipe(
      map((res)=>res))
  }

  getAssetsByID(id){
    return this.http.get(""+this.API_URL+"/v1/assets/"+id, options).pipe(
      map((res)=>res))
  }

  updateAssetsByID(asset:AssetModule){
    let body = JSON.stringify(asset);
    return this.http.put(""+this.API_URL+"/v1/assets", body, options).pipe(map((res)=>res))
  }

  deleteAssetById(id){
    return this.http.delete(""+this.API_URL+"/v1/assets/"+id,options).pipe(map((res)=>res));
  }

  getsensorsettingData(Page,Size){
    return this.http.get(""+this.API_URL+"/v1/sensors/setting/page/"+Page+"/pagesize/"+Size+"",options).pipe(map((res)=>res))
  }

  deletesensorsetting(id){
    let url:string = ""+this.API_URL+"/v1/sensors/setting/"+id
    return this.http.delete(url,options).pipe(map((res)=>res));

  }
  updatesensorsetting(id,type,minNum,maxNum,minValue,maxValue){
    let body = JSON.stringify({
      id:id,
      type:type,
      minNum:minNum,
      maxNum:maxNum,
      minValue:minValue,
      maxValue:maxValue
    });
    return this.http.put(""+this.API_URL+"/v1/sensors/setting", body,options).pipe(map((res)=>res))
  }

  mergeserverMapping(){
    return this.http.post(""+this.API_URL+"/v1/jobs/mergeservermapping",null,options).pipe(map((res)=>res))
  }

  mergePduServerMapping(){
    return this.http.post(""+this.API_URL+"/v1/jobs/pduservermapping",null,options).pipe(map((res)=>res))
  }

  fullSyncTempAndHumiditySensors(fullsync){
    return this.http.post(""+this.API_URL+"/v1/jobs/temphumiditymapping/fullsync/"+fullsync+"",null,options).pipe(map((res)=>res))
  }

  getUnmappedserver(){
    return this.http.get(""+this.API_URL+"/v1/assets/mapping/unmappedservers",options).pipe(
    map((res)=>res))
  }

  getSystemSummaryData(){
    return this.http.get(""+this.API_URL+"/v1/summary/systemsummary?usecache=true",options).pipe(map((res)=>res))
  }
  getAllVcenter(){
    return this.http.get(""+this.API_URL+"/v1/sddc/vc",options).pipe(map((res)=>res))
  }
  getVcenterById(id){
    return this.http.get(""+this.API_URL+"/v1/assets/mapping/vc/"+id,options).pipe(map((res)=>res))
  }
  getAssetById(id){
    return this.http.get(""+this.API_URL+"/v1/assets/"+id,options).pipe(map((res)=>res))
  }
  getExpiredTimeRange(){
    return this.http.get(""+this.API_URL+"/v1/setting/datapersistenttime",options).pipe(map((res)=>res))
  }
  updatesTimeRange(time:number){
    return this.http.put(""+this.API_URL+"/v1/setting/datapersistenttime/"+time, null,options).pipe(map((res)=>res))
  }
  getHostNameAndIPMapping(pageNumber:number,pagesize:number,searchIP:string){
    let url = ""+this.API_URL+"/v1/assets/mapping/hostnameip?pagesize="+pagesize+"&pagenumber="+pageNumber;
    if(searchIP != null){
      url = ""+this.API_URL+"/v1/assets/mapping/hostnameip?pagesize="+pagesize+"&pagenumber="+pageNumber+"&ip="+searchIP+"";
    }
    return this.http.get(url,options).pipe(map((res)=>res))
  }
  saveHostNameAndIPMapping(mapping:HostNameAndIpmappingModule){
    let body = JSON.stringify(mapping);
    return this.http.post(""+this.API_URL+"/v1/assets/mapping/hostnameip", body,options).pipe(map((res)=>res))
  }
  deleteHostNameAndIPMapping(id:string){
    return this.http.delete(""+this.API_URL+"/v1/assets/mapping/hostnameip/"+id,options).pipe(map((res)=>res))
  }
  updateHostNameAndIPMapping(mapping:HostNameAndIpmappingModule){
    let body = JSON.stringify(mapping);
    return this.http.put(""+this.API_URL+"/v1/assets/mapping/hostnameip", body,options).pipe(map((res)=>res))
  }
  searchAssetNameList(content:string){
    return this.http.get(""+this.API_URL+"/v1/assets/names/?queryParam="+content,options);
  }
}
