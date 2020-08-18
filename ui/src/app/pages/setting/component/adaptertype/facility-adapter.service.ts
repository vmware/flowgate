/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Injectable } from '@angular/core';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import 'rxjs/add/operator/map';
import { AuthenticationService } from '../../../auth/authenticationService';
import {environment} from 'environments/environment.prod';
import { FacilityAdapterModule } from './facility-adapter.module';

@Injectable()
export class FacilityAdapterService {

  private API_URL = environment.API_URL;
  private headers = new Headers({ 'Content-Type': 'application/json' });
  private options:RequestOptions;

  constructor(private http:Http,private auth:AuthenticationService) { 
   
  }
    
  createFacilityAdapter(adapter:FacilityAdapterModule){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    
    let body = JSON.stringify(adapter);
    return this.http.post(""+this.API_URL+"/v1/facilityadapter", body,this.options).map((res)=>res)
  }

  updateFacilityAdapter(adapter:FacilityAdapterModule){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    
    let body = JSON.stringify(adapter);
    return this.http.put(""+this.API_URL+"/v1/facilityadapter", body,this.options).map((res)=>res)
  }

  getAdapterByPagee(currentPage, pageSize){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header }); 
    
    return this.http.get(""+this.API_URL+"/v1/facilityadapter/pagenumber/"+currentPage+"/pagesize/"+pageSize, this.options)
      .map((res)=>res)
  }

  deleteAdapterById(id){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });

    return this.http.delete(""+this.API_URL+"/v1/facilityadapter/"+id,this.options).map((res)=>res);
  }

 
}
