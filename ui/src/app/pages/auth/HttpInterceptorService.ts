/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
 import { Injectable } from '@angular/core';
 import { Http, Request, RequestOptionsArgs, Response, RequestOptions, ConnectionBackend, Headers } from '@angular/http';
 import 'rxjs/Rx';
 import { Observable } from 'rxjs/Observable';
 import { AuthenticationService } from './authenticationService';
 import { Router } from '@angular/router';
 import {environment} from 'environments/environment.prod';
 @Injectable()
 export class HttpInterceptorService extends Http {
    private http: Http;
    private router: Router;
    private Auth_URL = environment.API_URL;
   constructor(backend: ConnectionBackend, defaultOptions: RequestOptions) {
     super(backend, defaultOptions);
   }
   request(url: string | Request, options ? : RequestOptionsArgs): Observable < Response > {
     
     return this.intercept(super.request(url, options));
   }
   get(url: string, options ? : RequestOptionsArgs): Observable < Response > {
            return super.get(url, options);
   }
   post(url: string, body: string, options ? : RequestOptionsArgs): Observable < Response > {
      
     return super.post(url, body, options);
   }

   RefactorRequestOptionArgs(options ? : RequestOptionsArgs): RequestOptionsArgs {
     if (options == null) {
       options = new RequestOptions();
     }
     if (options.headers == null) {
       options.headers = new Headers();
     }
     options.headers.append('Content-Type', 'application/json');
     const userStr = localStorage.getItem('currentUser');
     if(userStr!=null && userStr.trim() != ""){
        let token = JSON.parse(userStr).token.access_token;
        options.headers.append("Authorization",'Bearer ' + token);
     }
     return options;
   }
   intercept(observable: Observable < Response > ) : Observable < Response > {
       return observable.catch((err, source) => {
           if(err.status == 401 && err.json().error == "Unauthorized"){
               this.logout();
           }
           return Observable.throw(err);
      });

   }
   logout(): void {
    localStorage.removeItem('currentUser'); 
    window.location.href = "/";
  }
 }

