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
import { FacilityModule } from '../../facility.module';

@Injectable()
export class DcimService {
  //private API_URL = window.sessionStorage.getItem("api_url");
  private API_URL = environment.API_URL;
  private headers = new Headers({ 'Content-Type': 'application/json' });
  private options = new RequestOptions({ headers: this.headers });

  constructor(private http:Http,private auth:AuthenticationService) {
    //this.headers.append("Authorization",'Bearer ' + auth.getToken());
    //this.options = new RequestOptions({ headers: this.headers });
   }
  AddDcimConfig(dcim:FacilityModule){
 
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    let body = JSON.stringify(dcim);
    return this.http.post(""+this.API_URL+"/v1/facilitysoftware", body,this.options).map((res)=>res)
    }

    deleteDcimConfig(id){
    
      let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
      let url:string = ""+this.API_URL+"/v1/facilitysoftware/"+id
      return this.http.delete(url,this.options).map((res)=>res);
  
    }

    getDcimConfig(id){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/facilitysoftware/"+id+"",this.options)
        .map((res)=>res)
    }

    updateFacility(dcim:FacilityModule){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      let body = JSON.stringify(dcim);
      return this.http.put(""+this.API_URL+"/v1/facilitysoftware", body,this.options).map((res)=>res)
    }

    getDcimConfigData(pageNumber,pageSize){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/facilitysoftware/page/"+pageNumber+"/pagesize/"+pageSize+"",this.options)
      .map((res)=>res)
    }


    syncData(id){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.post(""+this.API_URL+"/v1/facilitysoftware/syncdatabyserverid/"+id+"",null,this.options).map((res)=>res)
    }
}
