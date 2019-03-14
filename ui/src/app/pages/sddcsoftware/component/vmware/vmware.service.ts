/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Injectable } from '@angular/core';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import 'rxjs/add/operator/map';
import { AuthenticationService } from '../../../auth/authenticationService';
import {environment} from 'environments/environment.prod';

@Injectable()
export class VmwareService {
  //private API_URL = window.sessionStorage.getItem("api_url");
  private API_URL = environment.API_URL;
  private headers = new Headers({ 'Content-Type': 'application/json' });
  private options:RequestOptions;

  constructor(private http:Http,private auth:AuthenticationService) { 
    //this.headers.append("Authorization",'Bearer ' + auth.getToken());
    //this.options = new RequestOptions({ headers: this.headers });
  }

  AddVmwareConfig(type,name,description,userName,password,serverURL,verifyCert){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    let body = JSON.stringify({
      type:type,
      name:name,
      description:description,
      userName:userName,
      password:password,
      serverURL:serverURL,
      verifyCert:verifyCert
   
    });
   
    return this.http.post(""+this.API_URL+"/v1/sddc", body,this.options).map((res)=>res)
    }

    deleteVmwareConfig(id){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
     
      let url:string = ""+this.API_URL+"/v1/sddc/"+id
      return this.http.delete(url,this.options).map((res)=>res);
  
    }

    getVmwareConfig(id){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/sddc/"+id+"",this.options)
        .map((res)=>res)
    }

    updateVmwareConfig(id,type,name,description,userName,password,serverURL,verifyCert){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      let body = JSON.stringify({
        id:id,
        type:type,
        name:name,
        description:description,
        userName:userName,
        password:password,
        serverURL:serverURL,
        verifyCert:verifyCert
      });
      
      return this.http.put(""+this.API_URL+"/v1/sddc", body,this.options).map((res)=>res)
    }

    getVmwareConfigData(pageNumber,pageSize){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/sddc/page/"+pageNumber+"/pagesize/"+pageSize+"",this.options)
      .map((res)=>res)
    }

    syncData(id){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.post(""+this.API_URL+"/v1/sddc/syncdatabyserverid/"+id+"",null,this.options).map((res)=>res)
    }

}
