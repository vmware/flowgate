/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { UserService } from '../../user.service';
import {ActivatedRoute,Router} from "@angular/router";

import { AuthenticationService } from '../../../auth/authenticationService';

@Component({
  selector: 'app-user-profile',
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss']
})
export class UserProfileComponent implements OnInit {

  constructor(private service:UserService, 
    private router:Router,
    private activedRoute:ActivatedRoute,
    private authservice:AuthenticationService
    ) { }
  modal="";
  basic=false;
  validconfirmPassword = false;
  user = {
    id:null,
    username:"",
    password:"",
    rpassword:"",
    email:""
  }
  
  checkadmin(){
    if(this.user.username == "admin"){
      return true;
    }
    return false;
  }

  getValidationState(){
    return this.validconfirmPassword;
  }
  handleValidation(key: string, flag: boolean): void {
    if(flag){
      if(this.user.password === this.user.rpassword){
        this.validconfirmPassword = false;
        }else{
          this.validconfirmPassword = true;
        }
    }
  }
  save(){
    let reg = /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?!.*\s).{8,20}$/;
    if(this.user.password != null && this.user.password != ""){
      if(this.user.password != this.user.rpassword){
        return;
      }else if(!reg.test(this.user.password)){
        return;
      }
    }
  
    this.service.updateUser(this.user.id,this.user.username,this.user.password,this.user.email,null).subscribe(
      (data)=>{
        this.router.navigate(["/ui/nav/"]);
      },
      error=>{
        this.basic = true;
        this.modal = error.json().message;
      }
    )
    
  }
  close(){
    this.basic = false;
    this.modal = "";
  }
  back(){
    this.router.navigateByUrl("/ui/nav/user/user-list");
  }

  getusers(){
    this.service.getUserByName().subscribe(
      (data)=>{
        this.user.id = data['id'];
        this.user.username =  data['userName'];
        this.user.email =  data['emailAddress'];   
      }
    )
  }
 
  ngOnInit() { 
    this.getusers(); 
  }

}
