/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { UserService } from '../../user.service';
import { Router } from "@angular/router";
import { FormBuilder, FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';


function passwordMatchValidator(password: string): ValidatorFn {
  return (control: FormControl) => {
    if (!control || !control.parent) {
      return null;
    }
    return control.parent.get(password).value === control.value ? null : { mismatch: true };
  };
}

@Component({
  selector: 'app-user-profile',
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss']
})
export class UserProfileComponent implements OnInit {
  editUserForm:FormGroup;
  constructor(private service:UserService, private router:Router,private fb: FormBuilder) { 
    this.editUserForm = this.fb.group({
      username: [{value:'',disabled: true}, [
        Validators.required
      ]],
      email:['', [
        Validators.required,
        Validators.email
      ]],
      password: ['', [
        Validators.pattern(/^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?!.*\s).{8,20}$/)
      ]],
      repassword: ['', [
        passwordMatchValidator('password')
      ]]
    })
  }
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

  save(){
    let reg = /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?!.*\s).{8,20}$/;
    if(this.user.password != null && this.user.password != ""){
      if(this.user.password != this.user.rpassword){
        return;
      }else if(!reg.test(this.user.password)){
        return;
      }
    }
    this.user.username = this.editUserForm.get("username").value;
    this.user.email = this.editUserForm.get("email").value;
    this.user.password = this.editUserForm.get("password").value;

    this.service.updateUser(this.user.id,this.user.username,this.user.password,this.user.email,null).subscribe(
      (data)=>{
        this.router.navigate(["/ui/nav/"]);
      },(error:HttpErrorResponse)=>{
        this.basic = true;
        this.modal = error.error.message;
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
         this.editUserForm.get('username').setValue(data['userName']) ;
        this.editUserForm.get('email').setValue(data['emailAddress']) ;
      }
    )
  }
 
  ngOnInit() { 
    this.getusers(); 
  }

}
