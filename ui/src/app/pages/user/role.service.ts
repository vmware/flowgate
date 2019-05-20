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

@Injectable()
export class RoleService {
  //private API_URL = window.sessionStorage.getItem("api_url");
  private API_URL = environment.Auth_URL;
  private headers = new Headers({ 'Content-Type': 'application/json' });
  private options = new RequestOptions({ headers: this.headers });

  constructor(private http:Http,private auth:AuthenticationService) { 
    //this.headers.append("Authorization",'Bearer ' + auth.getToken());
    //this.options = new RequestOptions({ headers: this.headers });
  }
  AddRole(roleName,privileges){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    let body = JSON.stringify({
      roleName:roleName,
      privilegeNames:privileges
    });
    
    return this.http.post(""+this.API_URL+"/v1/auth/role", body,this.options).map((res)=>res)
    }

    deleteRole(id){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      let url:string = ""+this.API_URL+"/v1/auth/role/"+id
      return this.http.delete(url,this.options).map((res)=>res);
  
    }

    getRole(id){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/auth/role/"+id+"",this.options)
        .map((res)=>res)
    }

    getPrivileges(){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/auth/privileges",this.options)
        .map((res)=>res)
    }

    updateRole(id,roleName,privileges){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      let body = JSON.stringify({
        id:id,
        roleName:roleName,
        privilegeNames:privileges
      });
      
      return this.http.put(""+this.API_URL+"/v1/auth/role", body,this.options).map((res)=>res)
    }

    getRoleData(Page,Size){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.API_URL+"/v1/auth/role?currentPage="+Page+"&pageSize="+Size+"",this.options).map((res)=>res)
    }
}
