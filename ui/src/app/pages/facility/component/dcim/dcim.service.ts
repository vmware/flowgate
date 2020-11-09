
/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { AuthenticationService } from '../../../auth/authenticationService';
import { environment } from 'environments/environment.prod';
import { FacilityModule } from '../../facility.module';
import { HttpClient,HttpHeaders } from '@angular/common/http';

const options = {
  headers:new HttpHeaders().set('Content-Type','application/json')
}
@Injectable()
export class DcimService {

  private API_URL = environment.API_URL;

  constructor(private http:HttpClient,private auth:AuthenticationService) {
   }

  AddDcimConfig(dcim:FacilityModule){
    let body = JSON.stringify(dcim);
    return this.http.post(""+this.API_URL+"/v1/facilitysoftware", body,options).pipe(map((res)=>res))
    }

    deleteDcimConfig(id){
      let url:string = ""+this.API_URL+"/v1/facilitysoftware/"+id
      return this.http.delete(url,options).pipe(map((res)=>res));
  
    }

    getDcimConfig(id){
      return this.http.get(""+this.API_URL+"/v1/facilitysoftware/"+id+"",options).pipe(
        map((res)=>res))
    }

    updateFacility(dcim:FacilityModule){
      let body = JSON.stringify(dcim);
      return this.http.put(""+this.API_URL+"/v1/facilitysoftware", body,options).pipe(map((res)=>res))
    }

    updateFacilityStatus(dcim:FacilityModule){
      let body = JSON.stringify(dcim);
      return this.http.put(""+this.API_URL+"/v1/facilitysoftware/status", body,options).pipe(map((res)=>res))
    }

    getDcimConfigData(pageNumber,pageSize,types){
      return this.http.get(""+this.API_URL+"/v1/facilitysoftware/page/"+pageNumber+"/pagesize/"+pageSize+"?softwaretypes="+types+"",options).pipe(
      map((res)=>res))
    }

    syncData(id){
      return this.http.post(""+this.API_URL+"/v1/facilitysoftware/syncdatabyserverid/"+id+"",null,options).pipe(map((res)=>res))
    }

    findAllFacilityAdapters(){
      return this.http.get(""+this.API_URL+"/v1/facilityadapter",options).pipe(
      map((res)=>res))
    }
}
