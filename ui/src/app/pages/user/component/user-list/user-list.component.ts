/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit,Input } from '@angular/core';
import { DataServiceService } from '../../../../data-service.service';
import {Router} from '@angular/router';
import { UserService } from '../../user.service';
import { ClrDatagridStateInterface } from '@clr/angular';
import { HttpErrorResponse } from '@angular/common/http';
@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss'],
  providers:[DataServiceService]
})
export class UserListComponent implements OnInit {
  @Input()auth;
  constructor(private data:UserService,private router: Router) { }
  users = [];
  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '5';
  info:string='';
  disabled:String="";
  basic:boolean = false;
  userId:string = '';
  clrAlertClosed:boolean = true;

  deleteUser:string[] = ["deleteUser"];
  updateUser:string[] = ["updateUser","readUserByID"];
 
  checkadmin(username:string){
    if(username == "admin"){
      return true;
    }
    return false;
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
    this.users = [];
    if (!state.page) {
      return;
    }
    this.currentState = state;
    this.getUsers(state.page.current,state.page.size);
  }
  getUsers(currentPage:number,pageSize:number){
    this.loading = true;
    this.data.getUserData(currentPage,pageSize).subscribe(
      (data)=>{
        this.loading = false;
        this.users = data['content'];
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

  toEditUser(){
    this.router.navigate(["/ui/nav/user/user-add"]);
  }
  onEdit(id){
    this.router.navigate(['/ui/nav/user/user-edit',id]);
  }
  confirm(){
    this.data.deleteUser(this.userId).subscribe(
      data=>{
        this.basic = false;
        this.refresh(this.currentState);
    },error=>{
        this.clrAlertClosed = false;
    })
  }
  onClose(){
    this.basic = false;
  }
  cancel(){
    this.basic = false;
    this.userId = "";
  }
  onDelete(id){
    this.basic = true;
    this.userId = id;
  }
  ngOnInit() {
  
  }
}