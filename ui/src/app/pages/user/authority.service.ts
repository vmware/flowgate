/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {map} from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { HttpClient,HttpHeaders } from '@angular/common/http';

const options = {
  headers:new HttpHeaders().set('Content-Type','application/json')
}

@Injectable()
export class AuthorityService {
  private Auth_URL = window.sessionStorage.getItem("auth_url");

  constructor(private http:HttpClient) { }
  AddAuthority(authContent,authUrl){
    let body = JSON.stringify({
      authContent:authContent,
      authUrl:authUrl
    });
   
    return this.http.post(""+this.Auth_URL+"/v1/auth/authority/", body,options).pipe(map((res)=>res))
    }

    deleteAuthority(id){
      let url:string = ""+this.Auth_URL+"/v1/auth/authority/"+id
      return this.http.delete(url,options).pipe(map((res)=>res));
  
    }

    getAuthority(id){
      return this.http.get(""+this.Auth_URL+"/v1/auth/authority/"+id+"").pipe(
        map((res)=>res))
    }

    updateAuthority(id,authContent,authUrl){
      let body = JSON.stringify({
        id:id,
        authContent:authContent,
        authUrl:authUrl
      });
     
      return this.http.put(""+this.Auth_URL+"/v1/auth/authority", body,options).pipe(map((res)=>res))
    }

    getAuthorityData(Page,Size){
      return this.http.get(""+this.Auth_URL+"/v1/auth/authority/page/"+Page+"/pagesize/"+Size+"").pipe(map((res)=>res))
    }

    getAuthorities(){
      return this.http.get(""+this.Auth_URL+"/v1/auth/authority/authorities").pipe(
      map((res)=>res))
    }

}
