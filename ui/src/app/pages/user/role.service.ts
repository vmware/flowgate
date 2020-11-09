/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {map} from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { AuthenticationService } from '../auth/authenticationService';
import {environment} from 'environments/environment.prod';
import { HttpClient,HttpHeaders } from '@angular/common/http';

const options = {
  headers:new HttpHeaders().set('Content-Type','application/json')
}
@Injectable()
export class RoleService {

  private API_URL = environment.Auth_URL;

  constructor(private http:HttpClient,private auth:AuthenticationService) { 
  }

  AddRole(roleName,privileges){
    let body = JSON.stringify({
      roleName:roleName,
      privilegeNames:privileges
    });
    return this.http.post(""+this.API_URL+"/v1/auth/role", body,options).pipe(map((res)=>res))
  }

  deleteRole(id){
    let url:string = ""+this.API_URL+"/v1/auth/role/"+id
    return this.http.delete(url,options).pipe(map((res)=>res));
  }

  getRole(id){
    return this.http.get(""+this.API_URL+"/v1/auth/role/"+id+"",options).pipe(
      map((res)=>res))
  }

  getPrivileges(){
    return this.http.get(""+this.API_URL+"/v1/auth/privileges",options).pipe(
      map((res)=>res))
  }

  updateRole(id,roleName,privileges){
    let body = JSON.stringify({
      id:id,
      roleName:roleName,
      privilegeNames:privileges
    });
    return this.http.put(""+this.API_URL+"/v1/auth/role", body,options).pipe(map((res)=>res))
  }

  getRoleData(Page,Size){
    return this.http.get(""+this.API_URL+"/v1/auth/role?currentPage="+Page+"&pageSize="+Size+"",options).pipe(map((res)=>res))
  }
}
