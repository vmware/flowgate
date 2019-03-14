/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component,OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DataServiceService } from '../../data-service.service';
import { LocalStorage } from '../../local.storage';
import { ActivatedRoute } from '@angular/router';
import { AuthenticationService } from '../auth/authenticationService';

@Component({
  selector: 'app-user-login',
  templateUrl: './user-login.component.html',
  styleUrls: ['./user-login.component.scss']
})
export class UserLoginComponent implements OnInit {

    tips:boolean = false;
    //auth:boolean = false;
    textContent = "";
   user ={
    id:"",
    username:"",
    password:"",
   }
    constructor(private router: Router,private data:AuthenticationService,private ls:LocalStorage) {
    }
    userName:string
    password:string 
    toLogin(){
        this.userName = this.user.username;
        this.password = this.user.password;
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
    // login(userName,password){
    //     this.data.login(userName,password,window.sessionStorage.getItem("auth_url")).subscribe(
    //         (data)=>{if( data.json().code == 1){
    //             this.tips = false;
    //             this.user.username = data.json().data.userName;
    //             this.user.id = data.json().data.id;
    //             this.auth = true;
    //             window.sessionStorage.setItem("username",this.user.username);
    //             window.sessionStorage.setItem("id",this.user.id);
    //             this.router.navigate(["nav"]);
    //         }else{
    //             this.tips = true;
    //             this.textContent = "Invalid user name or password";
    //             this.auth = false;
    //         }
    //       })      
    // }
    login(userName,password){
        this.data.login(userName,password).subscribe(
            (data)=>{if(data){
                this.tips = false;
                this.router.navigate(["ui/nav"]);
            }else{
                this.tips = true;
                this.textContent = "Invalid user name or password";
            }
          })      
    }
  ngOnInit() {
    
  }

}
