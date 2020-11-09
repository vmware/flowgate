
import {map} from 'rxjs/operators';
/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Injectable } from '@angular/core';
import { AuthenticationService } from '../../../auth/authenticationService';
import {environment} from 'environments/environment.prod';
import { SddcsoftwareModule } from '../../sddcsoftware.module';
import { HttpClient,HttpHeaders } from '@angular/common/http';

const options = {
  headers:new HttpHeaders().set('Content-Type','application/json')
}
@Injectable()
export class VmwareService {
  private API_URL = environment.API_URL;

  constructor(private http:HttpClient,private auth:AuthenticationService) { 
  }

  AddVmwareConfig(vmwareConfig:SddcsoftwareModule){
    let body = JSON.stringify(vmwareConfig);
    return this.http.post(""+this.API_URL+"/v1/sddc", body,options).pipe(map((res)=>res))
    }

    deleteVmwareConfig(id){
      let url:string = ""+this.API_URL+"/v1/sddc/"+id
      return this.http.delete(url,options).pipe(map((res)=>res));
    }

    getVmwareConfig(id){
      return this.http.get(""+this.API_URL+"/v1/sddc/"+id+"",options).pipe(
        map((res)=>res))
    }

    updateVmwareConfig(vmwareConfig:SddcsoftwareModule){
      let body = JSON.stringify(vmwareConfig);
      return this.http.put(""+this.API_URL+"/v1/sddc", body,options).pipe(map((res)=>res))
    }

    updateStatus(vmwareConfig:SddcsoftwareModule){
      let body = JSON.stringify(vmwareConfig);
      return this.http.put(""+this.API_URL+"/v1/sddc/status", body,options).pipe(map((res)=>res))
    }


    getVmwareConfigData(pageNumber,pageSize){
      return this.http.get(""+this.API_URL+"/v1/sddc/page/"+pageNumber+"/pagesize/"+pageSize+"",options).pipe(
      map((res)=>res))
    }

    syncData(id){
      return this.http.post(""+this.API_URL+"/v1/sddc/syncdatabyserverid/"+id+"",null,options).pipe(map((res)=>res))
    }

}
