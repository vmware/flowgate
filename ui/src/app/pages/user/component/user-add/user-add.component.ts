/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit,Input,Output, AfterViewChecked,EventEmitter, ChangeDetectorRef,ViewChild } from '@angular/core';
import { UserService } from '../../user.service';
import {ActivatedRoute,Router} from "@angular/router";
import { NgForm } from '@angular/forms';
@Component({
  selector: 'app-user-add',
  templateUrl: './user-add.component.html',
  styleUrls: ['./user-add.component.scss']
})
export class UserAddComponent implements AfterViewChecked,OnInit {
  constructor(private service:UserService,private router:Router,private activedRoute:ActivatedRoute, private ref: ChangeDetectorRef) { }

  @ViewChild("userForm") userForm: NgForm;
  userFormRef:NgForm;
  formValueChanged = false;
  validationStateMap: any = {};
  validconfirmPassword = false;
  timerHandler: any;
  alertclose:boolean = true;
  alertcontent:string = "";
  alertType:string = "";
  user = {
    id:"",
    username:"",
    password:"",
    rpassword:"",
    roleName:[],
    roleId:"",
    email:""
  }
  close(){
    this.alertclose = true;
    this.alertcontent = "";
    this.alertType = "";
  }
  resetState(): void {
    this.validationStateMap = {
        "password": true,
        "confirmPassword": true,
    };
}
  role={
    "id":"",
    "roleName":"",
    "privilegeNames":"",
    "enable":""
  }
  rolecheck:boolean = false;
  roles = [{
    "id":"",
    "roleName":"all",
    "privilegeNames":"",
    "enable":""
  }]

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
  getRoles(){
    this.service.getRoles().subscribe(
      (data)=>{
        var roles=[];
        roles.push({
          "id":"",
          "roleName":"all",
          "privilegeNames":"",
          "enable":""
         })
        data.json().forEach(element => {
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
      
      }
    )
  }

  save(){
    var rolenames = [];
    this.roles.forEach(element=>{
      if(element.enable != "" && element.roleName != "all"){
        rolenames.push(element.roleName);
      }
    })
      this.service.postuser(this.user.username,this.user.password,this.user.email,rolenames).subscribe(
        (data)=>{
          if(data.status == 201){
            this.alertclose = true;
            this.alertcontent = "";
            this.alertType = "";
            this.router.navigate(["/ui/nav/user/user-list"]);
          }
        },error=>{
          this.alertType = "alert-danger";
          this.alertclose = false;
          this.alertcontent = error.json().message;
        }
      )
  }

  reset(){
    this.user = {
      id:"",
      username:"",
      password:"",
      rpassword:"",
      roleName:[],
      roleId:"",
      email:""
    }
    this.alertclose = true;
    this.alertcontent = "";
    this.alertType = "";
  }
  back(){
    this.alertclose = true;
    this.alertcontent = "";
    this.alertType = "";
    this.router.navigate(["/ui/nav/user/user-list"]);
  }
  ngOnInit() {
    this.resetState();
    this.getRoles();
  }
 
  getValidationState(){
    return this.validconfirmPassword;
  }
  forceRefreshView(duration: number): void {
    // Reset timer
    if (this.timerHandler) {
      clearInterval(this.timerHandler);
    }
    this.timerHandler = setInterval(() => this.ref.markForCheck(), 100);
    setTimeout(() => {
      if (this.timerHandler) {
        clearInterval(this.timerHandler);
        this.timerHandler = null;
      }
    }, duration);
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
  ngAfterViewChecked(): void {
    if (this.userFormRef !== this.userForm) {
        this.userFormRef = this.userForm;
        if (this.userFormRef) {
            this.userFormRef.valueChanges.subscribe(data => {
                this.formValueChanged = true;
            });
        }
    }
  }



}
