/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Injectable } from '@angular/core';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import 'rxjs/add/operator/map'
@Injectable()
export class DataServiceService {

  private headers = new Headers({ 'Content-Type': 'application/json' });
  private options = new RequestOptions({ headers: this.headers });

  constructor(private http:Http) {
   }

  login(username,password,Auth_URL){
    return this.http.post(""+Auth_URL+"/tologin?userName="+username+"&password="+password+"",this.options).map((res)=>res)
  }
}
