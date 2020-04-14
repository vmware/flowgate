/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Injectable } from '@angular/core';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import 'rxjs/add/operator/map'
import { AuthenticationService } from '../auth/authenticationService';
import {environment} from 'environments/environment.prod';
import { Observable } from 'rxjs';
import { AssetModule } from './asset.module';

@Injectable()
export class ServermappingService {
  private API_URL = environment.API_URL;
  private headers = new Headers({ 'Content-Type': 'application/json' });
  private options:RequestOptions;

  constructor(private http:Http,private auth:AuthenticationService) {

   }
  getserverconfigs(type){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    return this.http.get(""+this.API_URL+"/v1/sddc/type/"+type+"",this.options)
    .map((res)=>res)
  }

  getServerMappings(pageSize,pageNumber,type,softwareId){ 
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });

    if(type == "VRO" || type=="VROPSMP"){
      return this.http.get(""+this.API_URL+"/v1/assets/mapping/vrops/"+softwareId+"/page/"+pageNumber+"/pagesize/"+pageSize+"",this.options)
      .map((res)=>res)
    }else if(type == "VCENTER"){
      return this.http.get(""+this.API_URL+"/v1/assets/mapping/vc/"+softwareId+"/page/"+pageNumber+"/pagesize/"+pageSize+"",this.options)
      .map((res)=>res)
    }
   
  }

  getAssets(pageSzie,pageNumber,keywords,category){
  let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    let url = "";
    if(keywords == ""){
      url = ""+this.API_URL+"/v1/assets/page/"+pageNumber+"/pagesize/"+pageSzie+"?category="+category;
    }else{
      url = ""+this.API_URL+"/v1/assets/page/"+pageNumber+"/pagesize/"+pageSzie+"/keywords/"+keywords+"?category="+category;
    }
    return this.http.get(url,this.options)
    .map((res)=>res)
  }

  getAssetById(id:string):Observable<any>{
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    return this.http.get(""+this.API_URL+"/v1/assets/"+id,this.options);
  }

  updateAsset(asset:AssetModule){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    let body = JSON.stringify(asset);
  
    return this.http.put(""+this.API_URL+"/v1/assets", body,this.options).map((res)=>res)
  }
  getMappingById(id:string){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    return this.http.get(""+this.API_URL+"/v1/assets/mapping/"+id,this.options).map((res)=>res)
  }
  updateServerMapping(id,assetID){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    let body = JSON.stringify({
      id:id,
      asset:assetID
    });
    
    return this.http.put(""+this.API_URL+"/v1/assets/mapping", body,this.options).map((res)=>res)
  }
  deleteServerMapping(id){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    return this.http.delete(""+this.API_URL+"/v1/assets/mapping/"+id,this.options).map((res)=>res)
  }
}