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
export class UserService {

  //private Auth_URL = window.sessionStorage.getItem("api_url");
  private Auth_URL = environment.API_URL;
  private headers = new Headers({ 'Content-Type': 'application/json' });
  private options:RequestOptions;

  constructor(private http:Http,private auth:AuthenticationService) {
    
   }

   getRoles(){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
    return this.http.get(""+this.Auth_URL+"/v1/auth/roles",this.options).map((res)=>res)
   }
    postuser(name,password,email,roleName){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
    let body = JSON.stringify({
      userName:name,
      password:password,
      emailAddress:email,
      roleNames:roleName
    });
    
    return this.http.post(""+this.Auth_URL+"/v1/auth/user", body,this.options).map((res)=>res)
    }

    getUser(id){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.Auth_URL+"/v1/auth/user/"+id+"",this.options)
        .map((res)=>res)
    }

    getUserByName(){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      let name = this.auth.getUsername();
      return this.http.get(""+this.Auth_URL+"/v1/auth/user/username/"+name+"",this.options)
        .map((res)=>res)
    }

    getUserData(Page,Size){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      return this.http.get(""+this.Auth_URL+"/v1/auth/user/page?currentPage="+Page+"&pageSize="+Size+"",this.options).map((res)=>res)
    }

  deleteUser(id){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.auth.getToken());
    this.options = new RequestOptions({ headers: header });
   
    let url:string = ""+this.Auth_URL+"/v1/auth/user/"+id
    return this.http.delete(url,this.options).map((res)=>res);

  }
    updateUser(id,name,password,email,roleName){
      let header = new Headers({ 'Content-Type': 'application/json' });
      header.append("Authorization",'Bearer ' + this.auth.getToken());
      this.options = new RequestOptions({ headers: header });
      let body = JSON.stringify({
        id:id,
        userName:name,
        password:password,
        emailAddress:email,
        roleNames:roleName
      });
     
      return this.http.put(""+this.Auth_URL+"/v1/auth/user", body,this.options).map((res)=>res)
    }
}
