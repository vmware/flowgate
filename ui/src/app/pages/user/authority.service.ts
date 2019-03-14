/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Injectable } from '@angular/core';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import 'rxjs/add/operator/map'

@Injectable()
export class AuthorityService {
  private Auth_URL = window.sessionStorage.getItem("auth_url");
  private headers = new Headers({ 'Content-Type': 'application/json' });
  private options = new RequestOptions({ headers: this.headers });

  constructor(private http:Http) { }
  AddAuthority(authContent,authUrl){
    let body = JSON.stringify({
      authContent:authContent,
      authUrl:authUrl
    });
   
    return this.http.post(""+this.Auth_URL+"/v1/auth/authority/", body,this.options).map((res)=>res)
    }

    deleteAuthority(id){
    
     
      let url:string = ""+this.Auth_URL+"/v1/auth/authority/"+id
      return this.http.delete(url,this.options).map((res)=>res);
  
    }

    getAuthority(id){
      return this.http.get(""+this.Auth_URL+"/v1/auth/authority/"+id+"")
        .map((res)=>res)
    }

    updateAuthority(id,authContent,authUrl){
      let body = JSON.stringify({
        id:id,
        authContent:authContent,
        authUrl:authUrl
      });
     
      return this.http.put(""+this.Auth_URL+"/v1/auth/authority", body,this.options).map((res)=>res)
    }

    getAuthorityData(Page,Size){
      
      return this.http.get(""+this.Auth_URL+"/v1/auth/authority/page/"+Page+"/pagesize/"+Size+"").map((res)=>res)
    }

    getAuthorities(){
      return this.http.get(""+this.Auth_URL+"/v1/auth/authority/authorities")
      .map((res)=>res)
    }

}
