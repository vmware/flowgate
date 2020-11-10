/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { UserService } from '../../user.service';
import {ActivatedRoute,Router} from "@angular/router";
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
  selector: 'app-user-edit',
  templateUrl: './user-edit.component.html',
  styleUrls: ['./user-edit.component.scss']
})

export class UserEditComponent implements OnInit {
  editUserForm:FormGroup;
  constructor(private service:UserService,private router:Router,private activedRoute:ActivatedRoute,private fb: FormBuilder) { 
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
    id:"",
    username:"",
    password:"",
    rpassword:"",
    roleNames:[],
    roleId:"",
    email:""
  }
  
  roles = [
    {
      "id":"",
      "roleName":"",
      "privilegeNames":"",
      "enable":""
    }
  ];
  addrole(roleName:string,ischecked:Boolean){
    
    if(roleName == "all"){
      this.roles.forEach(element=>{
        if(element.roleName != "all"){
          if(ischecked == false){
            element.enable="";
          }else{
            element.enable=ischecked+"";
          }
        }
      })
    }else{
      if(ischecked == false){
        this.roles.forEach(element=>{
          if(element.roleName == "all"){
            element.enable="";
          }
        })
      }else{
        var hasRoleNumber = 0;
        this.roles.forEach(element=>{
          if(element.enable != ""){
            hasRoleNumber++;
          }
        })
        if(hasRoleNumber == this.roles.length -1){
          this.roles.forEach(element=>{
            if(element.roleName == "all"){
              element.enable="true";
            }
          })
        }
     
      }
    } 
  }

  save(){
    var rolenames = [];
    this.roles.forEach(element=>{
      if(element.enable != "" && element.roleName != "all"){
        rolenames.push(element.roleName);
      }
    })
    this.user.email = this.editUserForm.get("email").value;
    this.user.password = this.editUserForm.get("password").value;

    this.service.updateUser(this.user.id,this.user.username,this.user.password,this.user.email,rolenames).subscribe(
      (data)=>{
        this.router.navigate(["/ui/nav/user/user-list"]);
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
    this.service.getUser(this.user.id).subscribe(
      (data)=>{
        this.editUserForm.get('username').setValue(data['userName']) ;
        this.editUserForm.get('email').setValue(data['emailAddress']) ;
        this.user.roleNames = data['roleNames'];
        this.getRoles()
      }
    )
  }
  getRoles(){
    this.service.getRoles().subscribe(
      (data)=>{
        var roles=[];
        roles.push({
          "id":"",
          "roleName":"all",
          "privilegeNames":"",
          "enable":""
         });
         data['content'].forEach(element => {
         var role={
         "id":"",
         "roleName":"",
         "privilegeNames":"",
         "enable":""
        };
         role.id= element.id;
         role.enable = "";
         role.privilegeNames = element.privilegeNames;
         role.roleName = element.roleName;
         roles.push(role);
         this.roles = roles;  
         });
         var hasrRoleCount = 0;
         this.user.roleNames.forEach(element=>{
         this.roles.forEach(element1=>{
           if(element1.roleName == element){
            element1.enable="true";
            hasrRoleCount++;
           }
          })
        })
        if(hasrRoleCount == this.roles.length-1){
          this.roles.forEach(element=>{
            if(element.roleName == "all"){
              element.enable="true";
            }
           })
        }
      }
      
    )

  }
  ngOnInit() { 
    this.user.id = this.activedRoute.snapshot.params['id'];
    if(this.user.id != null && this.user.id != ""){
      this.getusers();
    }
   
  }

}
