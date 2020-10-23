/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, ViewChild,OnInit, SystemJsNgModuleLoader } from '@angular/core';
import {Router,ActivatedRoute} from '@angular/router';
import { error } from 'util';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import { RoleService } from '../../role.service';
import {ClrWizard} from "@clr/angular";

@Component({
  selector: 'app-role-list',
  templateUrl: './role-list.component.html',
  styleUrls: ['./role-list.component.scss']
})
export class RoleListComponent implements OnInit {

  constructor(private http:Http,private service:RoleService,private router: Router, private route: ActivatedRoute) { }
  
  checkadmin(rolename:string){
    if(rolename == "admin"){
      return true;
    }
    return false;
  }
  //wizard
  @ViewChild("wizardxl") wizardExtraLarge: ClrWizard;
   editwizardOpen: boolean = false;
   @ViewChild("addwizard") addwizard: ClrWizard;
   addwizardOpen: boolean = false;

   @ViewChild("formPageOne") formData: any;
   loadingFlag: boolean = false;
   errorFlag: boolean = false;
   errorMessage:string = "";
   doCancel(): void {
      this.addwizard.close();
   }

  onCommit(): void {
      let value: any = this.formData.value;
      this.loadingFlag = true;
      this.errorFlag = false;

      setTimeout(() => {
        this.service.AddRole(this.role.roleName,this.rolePrivilege).subscribe(
          (data)=>{
            if(data.status == 201){
              this.addwizard.close();
              this.getroledatas(this.currentPage,this.pageSize);
              this.role.roleName = "";
              this.errorMessage = "";
            }
          },(error)=>{
            this.errorMessage = error.json().message;
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



  setInfo(){
    this.info=this.pageSize;
    this.getroledatas(this.currentPage,this.pageSize)
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.getroledatas(this.currentPage,this.pageSize)
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++
      this.getroledatas(this.currentPage,this.pageSize)
    }
  }
  createTime(time){
		var da = time;
	    da = new Date(da);
	    var year = da.getFullYear()+'-';
	    var month = da.getMonth()+1+'-';
	    var date = da.getDate();
	    return year+month+date;
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
    this.role.roleName = "";
    this.systemprivilegeselected = [];
    this.roleprivilegeselected = [];
    this.getroledatas(this.currentPage,this.pageSize)
  }

  save(){
    this.service.updateRole(this.role.id,this.role.roleName,this.rolePrivilege).subscribe(
      (data)=>{
        if(data.status == 200){
          this.getroledatas(this.currentPage,this.pageSize)
        }
      }
    )
    this.role.id = "";
    this.role.roleName = "";
    this.systemprivilegeselected = [];
    this.roleprivilegeselected = [];
  }
  onEdit(roleId:string,roleName:string,privileges:string[]){
    this.role.id = roleId;
    this.role.roleName = roleName;
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
    this.service.deleteRole(this.roleId).subscribe(data=>{
      
      if(data.status == 200){
        this.basic = false;
        this.getroledatas(this.currentPage,this.pageSize);
        this.deleteOptionTipClosed = true;
      }
    },
    error=>{
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
      (data)=>{if(data.status == 200){
            this.privilegeNames = data.json()
      }
    })
  }
  alertclose:boolean = true;
  alertType:string = "";
  alertcontent:string = "";
  close(){
    this.alertclose = true;
  }
  
  loading:boolean = true;

  getroledatas(currentPage,pageSize){
    this.loading = true;
    this.service.getRoleData(currentPage,pageSize).subscribe(
      (data)=>{
        if(data.status == 200){
          this.loading = false;
          this.roles = data.json().content
          this.currentPage = data.json().number+1;
          this.totalPage = data.json().totalPages
          if(this.totalPage == 1){
            this.disabled = "disabled";
          }else{
            this.disabled = "";
          }
      }
    },(error)=>{
      this.loading = false;
      this.alertType = "alert-danger";
      this.alertcontent = "Internal error";
      if(error._body != null && error.status != "0"){
        this.alertcontent = error.json().message;
      }
      this.alertclose = false;
    })
  }
  ngOnInit() {
     this.getroledatas(this.currentPage,this.pageSize); 
     this.getprivileges();
  }

}
