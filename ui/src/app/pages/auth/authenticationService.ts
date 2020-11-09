/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {map} from 'rxjs/operators';
import {Injectable} from '@angular/core';
import {environment} from 'environments/environment.prod';
import { Router } from '@angular/router';
import { HttpClient,HttpHeaders } from '@angular/common/http';

@Injectable()
export class AuthenticationService {
  private authurl:string=environment.API_URL;
  constructor(private http: HttpClient,private router: Router) {
  }

  login(username: string, password: string){
    const options = {
      headers:new HttpHeaders().set('Content-Type','application/json')
    }
    return this.http.post(""+this.authurl+"/v1/auth/token", JSON.stringify({userName: username, password: password}), options).pipe(map((res)=>res))
  }

  getPrivileges(token:string){
    const headers = new HttpHeaders().append('Content-Type','application/json').append("Authorization",'Bearer ' + token);
    const options = {
      headers:headers
    }
    return this.http.get(""+this.authurl+"/v1/auth/privileges",options).pipe(
      map((res)=>res))
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
    const headers = new HttpHeaders().append('Content-Type','application/json');
    const options = {
      headers:headers
    }
    this.http.get(""+this.authurl+"/v1/auth/logout",options).subscribe(
      (data)=>{
          sessionStorage.removeItem('currentUser');
          this.router.navigateByUrl("/");
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