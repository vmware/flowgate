/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpErrorResponse, HttpResponse} from '@angular/common/http';
import {Observable, of, throwError} from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthenticationService } from './authenticationService';

@Injectable()
export class AuthenticationInterceptor implements HttpInterceptor {

  constructor(private service:AuthenticationService){

  }
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if(this.service.isLoggedIn()){
      req = req.clone({
        setHeaders: {
            'Authorization': 'Bearer ' +this.service.getToken()
        }
    });
    }

    if(req.url.lastIndexOf('/auth/token') != -1){
      return next.handle(req).pipe(
        catchError((err: HttpErrorResponse) => this.handleDataNoJump(err))
      );
    }

    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => this.handleData(err))
    );
  }

  private handleData(
    event: HttpResponse<any> | HttpErrorResponse,
  ): Observable<any> {
    
    switch (event.status) {
      case 401:
        window.location.href = "/";
        return of(event) ;
      default:
    }
    return throwError(event) ;
  }

  private handleDataNoJump(
    event: HttpResponse<any> | HttpErrorResponse,
  ): Observable<any> {
    return throwError(event) ;
  }
}