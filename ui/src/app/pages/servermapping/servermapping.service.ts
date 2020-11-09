/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {map} from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { AuthenticationService } from '../auth/authenticationService';
import { environment } from 'environments/environment.prod';
import { Observable } from 'rxjs';
import { AssetModule } from './asset.module';
import { HttpClient,HttpHeaders } from '@angular/common/http';


const options = {
  headers:new HttpHeaders().set('Content-Type','application/json')
}
@Injectable()
export class ServermappingService {
  private API_URL = environment.API_URL;

  constructor(private http:HttpClient,private auth:AuthenticationService) {

   }
  getserverconfigs(type){
    return this.http.get(""+this.API_URL+"/v1/sddc/type/"+type+"",options).pipe(
    map((res)=>res))
  }

  getServerMappings(pageSize,pageNumber,type,softwareId){ 
    if(type == "VRO" || type=="VROPSMP"){
      return this.http.get(""+this.API_URL+"/v1/assets/mapping/vrops/"+softwareId+"/page/"+pageNumber+"/pagesize/"+pageSize+"",options).pipe(
      map((res)=>res))
    }else if(type == "VCENTER"){
      return this.http.get(""+this.API_URL+"/v1/assets/mapping/vc/"+softwareId+"/page/"+pageNumber+"/pagesize/"+pageSize+"",options).pipe(
      map((res)=>res))
    }
   
  }

  getAssets(pageSzie,pageNumber,keywords,category){
    let url = "";
    if(keywords == ""){
      url = ""+this.API_URL+"/v1/assets/page/"+pageNumber+"/pagesize/"+pageSzie+"?category="+category;
    }else{
      url = ""+this.API_URL+"/v1/assets/page/"+pageNumber+"/pagesize/"+pageSzie+"/keywords/"+keywords+"?category="+category;
    }
    return this.http.get(url,options).pipe(
    map((res)=>res))
  }

  getAssetById(id:string):Observable<any>{
    return this.http.get(""+this.API_URL+"/v1/assets/"+id,options);
  }

  updateAsset(asset:AssetModule){
    let body = JSON.stringify(asset);
    return this.http.put(""+this.API_URL+"/v1/assets/mappingfacility", body,options).pipe(map((res)=>res))
  }
  getMappingById(id:string){
    return this.http.get(""+this.API_URL+"/v1/assets/mapping/"+id,options).pipe(map((res)=>res))
  }
  updateServerMapping(id,assetID){
    let body = JSON.stringify({
      id:id,
      asset:assetID
    });
    
    return this.http.put(""+this.API_URL+"/v1/assets/mapping", body,options).pipe(map((res)=>res))
  }
  deleteServerMapping(id){
    return this.http.delete(""+this.API_URL+"/v1/assets/mapping/"+id,options).pipe(map((res)=>res))
  }
}