/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, ViewChild,OnInit } from '@angular/core';
import { RoleService } from '../../role.service';
import {ClrDatagridStateInterface, ClrWizard} from "@clr/angular";
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-role-list',
  templateUrl: './role-list.component.html',
  styleUrls: ['./role-list.component.scss']
})
export class RoleListComponent implements OnInit {

  constructor(private service:RoleService,private fb: FormBuilder) { 
    this.addRoleForm = this.fb.group({
      roleName: ['', [
        Validators.required
      ]]
    });
    this.editRoleForm = this.fb.group({
      roleName: ['', [
        Validators.required
      ]]
    });
  }
  
  checkadmin(rolename:string){
    if(rolename == "admin"){
      return true;
    }
    return false;
  }
  //wizard
  @ViewChild("eidtwizard") editWizard: ClrWizard;
   editwizardOpen: boolean = false;
   @ViewChild("addwizard") addwizard: ClrWizard;
   addwizardOpen: boolean = false;

   addRoleForm:FormGroup;
   editRoleForm:FormGroup;
   loadingFlag: boolean = false;
   errorFlag: boolean = false;
   errorMessage:string = "";
   doCancel(): void {
      this.addwizard.close();
   }

  onCommit(): void {
      this.loadingFlag = true;
      this.errorFlag = false;

      setTimeout(() => {
        this.service.AddRole(this.addRoleForm.get('roleName').value,this.rolePrivilege).subscribe(
          (data)=>{
            this.addwizard.reset();
            this.addRoleForm.reset();
            this.addwizard.close();
            this.refresh(this.currentState);
            this.errorMessage = "";
          },(error:HttpErrorResponse)=>{
            this.errorMessage = error.error.message;
            this.errorFlag = true;
          }
        )
        this.role.id = "";
        this.systemprivilegeselected = [];
        this.roleprivilegeselected = [];
        this.loadingFlag = false;
       }, 1000);
  }
   role =  {
    id:"",
    roleName:"",
    privilegeNames:[]
  }
 
  roles = [
  ];
  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '5';
  info:string='';
  disabled:String="";
  basic:boolean = false;
  roleId:string = '';
  deleteOptionTipClosed:boolean = true;
  updateprivilege:boolean = false;
  addrole:string[]=["createRole"];
  updaterole:string[]=["readRole","updateRole"];
  deleterole:string[]=["deleteRole"];
  privilegeNames:string[]=[];//all privileges

  systemPrivileges:string[]=[];//privileges that do not belong to this role
  systemprivilegeselected:string[] = [];
  rolePrivilege:string[]=[];//privileges belonging to this role 
  roleprivilegeselected:string[] = [];


  showprivilege(privilege:string[]){
    this.rolePrivilege = privilege;
    this.updateprivilege =true;
  }

  getsystemPrivileges(privilege:string[]){
    this.privilegeNames.forEach(element=>{
      if(privilege.indexOf(element) == -1){
        this.systemPrivileges.push(element);
       }
    })
  }

  addprivilege(){
    this.systemprivilegeselected.forEach(element=>{
      this.rolePrivilege.push(element);
      this.systemPrivileges.splice(this.systemPrivileges.indexOf(element),1);
    })
      this.systemprivilegeselected = [];
  }

  reduceprivilege(){
    this.roleprivilegeselected.forEach(element=>{
      this.systemPrivileges.push(element);
      this.rolePrivilege.splice(this.rolePrivilege.indexOf(element),1);
    });
    this.roleprivilegeselected = [];
  }
  
  toAddrole(){
    this.addwizardOpen =true;
    this.rolePrivilege = [];
    this.systemPrivileges = [];
    this.privilegeNames.forEach(element=>{
      if(this.rolePrivilege.indexOf(element) == -1){
        this.systemPrivileges.push(element);
       }
    })
  }

  cancleUpdate(){
    this.role.id = "";
    this.systemprivilegeselected = [];
    this.roleprivilegeselected = [];
    this.editWizard.close();
    this.editWizard.reset();
    this.editRoleForm.reset();
    this.errorFlag = false;
    this.errorMessage = "";
  }
  cancleAdd(){
    this.role.roleName = "";
    this.addRoleForm.reset();
    this.addwizard.reset();
  }

  save(){
    this.loadingFlag = true;
    this.errorFlag = false;
    this.service.updateRole(this.role.id,this.editRoleForm.get('roleName').value,this.rolePrivilege).subscribe(
      (data)=>{
        this.editWizard.close();
        this.editWizard.reset();
        this.editRoleForm.reset();
        this.refresh(this.currentState);
        this.errorMessage = "";
        this.systemprivilegeselected = [];
        this.roleprivilegeselected = [];
        this.loadingFlag = false;
      },(error:HttpErrorResponse)=>{
        this.errorMessage = error.error.message;
        this.errorFlag = true;
        this.systemprivilegeselected = [];
        this.roleprivilegeselected = [];
        this.loadingFlag = false;
      }
    ) 

  }
  onEdit(roleId:string,roleName:string,privileges:string[]){
    this.role.id = roleId;
    this.editRoleForm.get('roleName').setValue(roleName);
    this.rolePrivilege = privileges;
    this.systemPrivileges = [];
    this.privilegeNames.forEach(element=>{
      if(privileges.indexOf(element) == -1){
        this.systemPrivileges.push(element);
       }
    })
    this.editwizardOpen =true;
    
  }
  confirmdelete(){
    this.service.deleteRole(this.roleId).subscribe(
      data=>{
        this.basic = false;
        this.refresh(this.currentState);
        this.deleteOptionTipClosed = true;
    }, error=>{
        this.deleteOptionTipClosed = false;
    })
  }

  canceldelete(){
    this.basic = false;
    this.deleteOptionTipClosed = true;
    this.roleId = "";
  }
  onDelete(id){
    this.basic = true;
    this.roleId = id;
  }

  getprivileges(){
    this.service.getPrivileges().subscribe(
      (data:string[])=>{
        this.privilegeNames = data;
    })
  }
  alertclose:boolean = true;
  alertType:string = "";
  alertcontent:string = "";
  close(){
    this.alertclose = true;
  }
  
  loading:boolean = true;
  currentState:ClrDatagridStateInterface;
  totalItems:number = 0;
  refresh(state: ClrDatagridStateInterface){
    this.roles = [];
    if (!state.page) {
      return;
    }
    this.currentState = state;
    let pagenumber = Math.round((state.page.from + 1) / state.page.size) + 1;
    this.getRoles(pagenumber,state.page.size);
  }
  getRoles(currentPage:number,pageSize:number){
    this.loading = true;
    this.service.getRoleData(currentPage,pageSize).subscribe(
      (data)=>{
        this.loading = false;
        this.roles = data['content'];
        this.totalItems = data['totalElements'];
    },(error:HttpErrorResponse)=>{
        this.loading = false;
        this.alertType = "danger";
        this.alertcontent = "Internal error";
        if(error.status != 0){
          this.alertcontent = error.error.message;
        }
        this.alertclose = false;
    })
  }
  ngOnInit() {
     this.getprivileges();
  }

}
