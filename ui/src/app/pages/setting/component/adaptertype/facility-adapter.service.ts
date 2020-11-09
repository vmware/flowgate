
/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {map} from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { AuthenticationService } from '../../../auth/authenticationService';
import {environment} from 'environments/environment.prod';
import { FacilityAdapterModule } from './facility-adapter.module';
import { HttpClient,HttpHeaders } from '@angular/common/http';

const options = {
  headers:new HttpHeaders().set('Content-Type','application/json')
}
@Injectable()
export class FacilityAdapterService {

  private API_URL = environment.API_URL;

  constructor(private http:HttpClient,private auth:AuthenticationService) { 
  }
    
  createFacilityAdapter(adapter:FacilityAdapterModule){
    let body = JSON.stringify(adapter);
    return this.http.post(""+this.API_URL+"/v1/facilityadapter", body,options).pipe(map((res)=>res))
  }

  updateFacilityAdapter(adapter:FacilityAdapterModule){
    let body = JSON.stringify(adapter);
    return this.http.put(""+this.API_URL+"/v1/facilityadapter", body,options).pipe(map((res)=>res))
  }

  getAdapterByPagee(currentPage, pageSize){
    return this.http.get(""+this.API_URL+"/v1/facilityadapter/pagenumber/"+currentPage+"/pagesize/"+pageSize, options).pipe(
      map((res)=>res))
  }

  deleteAdapterById(id){
    return this.http.delete(""+this.API_URL+"/v1/facilityadapter/"+id,options).pipe(map((res)=>res));
  }

}
