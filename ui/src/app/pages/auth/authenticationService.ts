/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import { of } from 'rxjs/observable/of';
import {catchError, tap} from 'rxjs/operators';
import {environment} from 'environments/environment.prod';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import { Router } from '@angular/router';

const httpOptions = {
  headers: new Headers({
    'Content-Type': 'application/json'})
};

@Injectable()
export class AuthenticationService {
  private authurl:string=environment.API_URL;
  private headers = new Headers({ 'Content-Type': 'application/json' });
  private options:RequestOptions;
  private currentUser:string;

  constructor(private http: Http,private router: Router) {
  }

  login(username: string, password: string){
    let header = new Headers({ 'Content-Type': 'application/json' });
    let options:RequestOptions = new RequestOptions({ headers: header });
    return this.http.post(""+this.authurl+"/v1/auth/token", JSON.stringify({userName: username, password: password}), options).map((res)=>res)
  }

  getPrivileges(token:string){
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + token);
    this.options = new RequestOptions({ headers: header });
    return this.http.get(""+this.authurl+"/v1/auth/privileges",this.options)
      .map((res)=>res)
  }

  getCurrentUser(): any {
    const userStr:string = sessionStorage.getItem('currentUser');
    const user = atob(userStr);
    return user ? JSON.parse(user) : '';
  }

  getToken(): string {
    const currentUser = this.getCurrentUser();
    return currentUser ? currentUser.token : '';
  }

  getExpiredTime(): number{
    const currentUser = this.getCurrentUser();
    return currentUser ? currentUser.expires_in : '';
  }

  getUsername(): string {
    const currentUser = this.getCurrentUser();
    return currentUser ? currentUser.username : '';
  }

  logout(): void {
    let header = new Headers({ 'Content-Type': 'application/json' });
    header.append("Authorization",'Bearer ' + this.getToken());
    this.options = new RequestOptions({ headers: header });
    this.http.get(""+this.authurl+"/v1/auth/logout",this.options).subscribe(
      (data)=>{
        if(data.status == 200){
          sessionStorage.removeItem('currentUser');
          this.router.navigateByUrl("/");
          
        }
      }
    )
 
  }

  isLoggedIn(): boolean {
    const userStr:string = sessionStorage.getItem('currentUser');
    if(userStr == null){
      return false;
    }
    const expriedTime:number = this.getExpiredTime();
    const currentTime:number = new Date().getTime();
    return expriedTime>currentTime;
  }

  hasRole(role: string): boolean {
    const currentUser = this.getCurrentUser();
    if (!currentUser) {
      return false;
    }
    const authorities: string[] = currentUser.authorities;
    return authorities.indexOf('ROLE_' + role) != -1;
  }

  hasAuthorities(authority : string[]): boolean{
    const currentUser = this.getCurrentUser();
    var res:boolean=false;
    if (!currentUser) {
      return false;
    }
    const authorities: string[] = currentUser.authorities;
    authority.forEach(element => {
     if(authorities.indexOf(element) != -1){
      res = true;
     }
    });
    return res;
  }
}