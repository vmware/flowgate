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
export class UserService {

  private Auth_URL = environment.API_URL;

  constructor(private http:HttpClient,private auth:AuthenticationService) {
    
   }

  getRoles(){
    return this.http.get(""+this.Auth_URL+"/v1/auth/role?currentPage=0&pageSize=2000",options).pipe(map((res)=>res))
  }
  postuser(name,password,email,roleName){
    let body = JSON.stringify({
      userName:name,
      password:password,
      emailAddress:email,
      roleNames:roleName
    });
    return this.http.post(""+this.Auth_URL+"/v1/auth/user", body,options).pipe(map((res)=>res))
  }

  getUser(id){
    return this.http.get(""+this.Auth_URL+"/v1/auth/user/"+id+"",options).pipe(
      map((res)=>res))
  }

  getUserByName(){
    let name = this.auth.getUsername();
    return this.http.get(""+this.Auth_URL+"/v1/auth/user/username/"+name+"",options).pipe(
      map((res)=>res))
  }

  getUserData(Page,Size){
    return this.http.get(""+this.Auth_URL+"/v1/auth/user?currentPage="+Page+"&pageSize="+Size+"",options).pipe(map((res)=>res))
  }

  deleteUser(id){
    let url:string = ""+this.Auth_URL+"/v1/auth/user/"+id
    return this.http.delete(url,options).pipe(map((res)=>res));

  }

  updateUser(id,name,password,email,roleName){
    let body = JSON.stringify({
      id:id,
      userName:name,
      password:password,
      emailAddress:email,
      roleNames:roleName
    });
    return this.http.put(""+this.Auth_URL+"/v1/auth/user", body,options).pipe(map((res)=>res))
  }
}
