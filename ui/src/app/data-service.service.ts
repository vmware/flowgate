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
export class DataServiceService {


  constructor(private http:HttpClient) {
   }

  login(username,password,Auth_URL){
    return this.http.post(""+Auth_URL+"/tologin?userName="+username+"&password="+password+"",options).pipe(map((res)=>res))
  }
}
