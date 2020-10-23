/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit,Input } from '@angular/core';
import { DataServiceService } from '../../../../data-service.service';
import {Router,ActivatedRoute} from '@angular/router';
import { error } from 'util';
import {Http,RequestOptions } from '@angular/http'
import { Headers, URLSearchParams } from '@angular/http';
import { UserService } from '../../user.service';
@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss'],
  providers:[DataServiceService]
})
export class UserListComponent implements OnInit {
  @Input()auth;
  constructor(private http:Http,private data:UserService,private router: Router, private route: ActivatedRoute) { }
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
  setInfo(){
    this.info=this.pageSize;
    this.getUserdatas(this.currentPage,this.pageSize)
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.getUserdatas(this.currentPage,this.pageSize)
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++
      this.getUserdatas(this.currentPage,this.pageSize)
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

  alertclose:boolean = true;
  alertType:string = "";
  alertcontent:string = "";
  close(){
    this.alertclose = true;
  }
  
  loading:boolean = true;
  getUserdatas(currentPage,pageSize){
    this.data.getUserData(currentPage,pageSize).subscribe(
      (data)=>{
        if(data.status == 200){
          this.loading = false;
          this.users = data.json().content;
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
  toEditUser(){
    this.router.navigate(["/ui/nav/user/user-add"]);
  }
  onEdit(id){
    this.router.navigate(['/ui/nav/user/user-edit',id]);
  }
  confirm(){
    this.data.deleteUser(this.userId).subscribe(data=>{
      if(data.status == 200){
        this.basic = false;
        this.getUserdatas(this.currentPage,this.pageSize)
      }else{
        this.clrAlertClosed = false;
      }
    },
    error=>{
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
     this.getUserdatas(this.currentPage,this.pageSize); 
  
  }
}