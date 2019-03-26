/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Injectable } from '@angular/core';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import 'rxjs/add/operator/map';
import { AuthenticationService } from '../auth/authenticationService';
import {environment} from 'environments/environment.prod';
@Injectable()
export class SettingService {

  //private API_URL = window.sessionStorage.getItem("api_url");
  private API_URL = environment.API_URL;
  private headers = new Headers({ 'Content-Type': 'application/json' });
  private options:RequestOptions;

  constructor(private http:Http,private auth:AuthenticationService) { 
    //this.headers.append("Authorization",'Bearer ' + auth.getToken());
    //this.options = new RequestOptions({ headers: this.headers });
  }
  postsensorsetting(type,minNum,maxNum,minValue,maxValue){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    let body = JSON.stringify({
      type:type,
      minNum:minNum,
      maxNum:maxNum,
      minValue:minValue,
      maxValue:maxValue
    });
   
    return this.http.post(""+this.API_URL+"/v1/sensors/setting", body,this.options).map((res)=>res)
    }

    getsensorsetting(id){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/sensors/setting/"+id+"",this.options)
        .map((res)=>res)
    }

    getsensorsettingData(Page,Size){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/sensors/setting/page/"+Page+"/pagesize/"+Size+"",this.options).map((res)=>res)
    }

  deletesensorsetting(id){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    let url:string = ""+this.API_URL+"/v1/sensors/setting/"+id
    return this.http.delete(url,this.options).map((res)=>res);

  }
    updatesensorsetting(id,type,minNum,maxNum,minValue,maxValue){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      let body = JSON.stringify({
        id:id,
        type:type,
        minNum:minNum,
        maxNum:maxNum,
        minValue:minValue,
        maxValue:maxValue
      });
      return this.http.put(""+this.API_URL+"/v1/sensors/setting", body,this.options).map((res)=>res)
    }


    mergeserverMapping(){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
        return this.http.post(""+this.API_URL+"/v1/jobs/mergeservermapping",null,this.options).map((res)=>res)
    }

    mergePduServerMapping(){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
        return this.http.post(""+this.API_URL+"/v1/jobs/pduservermapping",null,this.options).map((res)=>res)
    }

    getUnmappedserver(){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/assets/mapping/unmappedservers",this.options)
      .map((res)=>res)
    }

    getFirstPageDataServer(){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/dashboard/alldashboarddata",this.options).map((res)=>res)
    }
}
