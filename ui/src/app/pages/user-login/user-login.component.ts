/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { HttpErrorResponse } from '@angular/common/http';
import { Component,OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LocalStorage } from '../../local.storage';
import { AuthenticationService } from '../auth/authenticationService';

@Component({
  selector: 'app-user-login',
  templateUrl: './user-login.component.html',
  styleUrls: ['./user-login.component.scss']
})
export class UserLoginComponent implements OnInit {


    tips:boolean = false;
    textContent = "";
    user ={
        id:"",
        username:"",
        password:"",
        rememberMe:false
    }
    constructor(private router: Router,private data:AuthenticationService,private ls:LocalStorage) {
    }
    userName:string
    password:string
    rememberMe:boolean
    toLogin(){
        this.userName = this.user.username;
        this.password = this.user.password;
        this.rememberMe = this.user.rememberMe;

        if(this.userName == ""){
            this.tips = true;
            this.textContent = "please input a userName";
        }else if(this.password == ""){
            this.tips = true;
            this.textContent = "please input a password";
        }else{
            this.login(this.userName,this.password);
        }
    }

    login(userName,password){
        let userInfoBase64:string = "";
        let user:string = "";
        let privilegeName:string[] = [];
        this.data.login(userName,password).subscribe(
            (res)=>{
                userInfoBase64 = res['access_token'].split('.')[1];
                user = atob(userInfoBase64);
                this.data.getPrivileges(res['access_token']).subscribe(
                    (priData:string[])=>{
                        privilegeName = priData;
                        let currentUser = btoa(JSON.stringify({username: JSON.parse(user).sub, token: res['access_token'], authorities:privilegeName,expires_in:res['expires_in']}));
                        sessionStorage.setItem('currentUser', currentUser);
                        if (this.rememberMe) {
                            localStorage.setItem("username", btoa(userName));
                            localStorage.setItem("password", btoa(password));
                        }
                        this.tips = false;
                        this.router.navigate(["ui/nav"]);
                    }
                )
          },(error:HttpErrorResponse)=>{
            if(error.status == 401){
                this.tips = true;
                this.textContent = "Invalid user name or password";
            }else{
                this.tips = true;
                this.textContent = "Internal error";
            }
        })
    }

    ngOnInit() {
        let username = localStorage.getItem("username");
        let password = localStorage.getItem("password");
        if (username == null || password == null) {
            return
        }
        this.login(atob(username), atob(password));
    }

}
