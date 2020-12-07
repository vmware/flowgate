/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import {Router,ActivatedRoute} from '@angular/router';
import { ClrDatagridStateInterface } from '@clr/angular';
import { SettingService } from '../../setting.service';


@Component({
  selector: 'app-asset-list',
  templateUrl: './asset-list.component.html',
  styleUrls: ['./asset-list.component.scss']
})
export class AssetListComponent implements OnInit {

  constructor(private service:SettingService,private router: Router, private route: ActivatedRoute) { }
 
  assets = [];
  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '20';
  info:string='';
  disabled:String="";
  basic:boolean = false;
  assetId:string = '';
  clrAlertClosed:boolean = true;

  loading:boolean = true;
  currentState:ClrDatagridStateInterface;
  totalItems:number = 0;
  refresh(state: ClrDatagridStateInterface){
    this.assets = [];
    this.loading = true;
    if (!state.page) {
      return;
    }
    this.currentState = state;
    this.getAssetsDatas(state.page.current,state.page.size);
  }

  getAssetsDatas(currentPage:number,pageSize:number){
    this.assets = [];
    this.service.getAssetsBySource("flowgate", currentPage, pageSize).subscribe(
      (data)=>{
        this.assets =  data['content'];
        this.totalItems = data['totalElements'];
        this.loading = false;
      },(error)=>{
        this.loading = false;
      })
  }
  addAsset(){
    this.router.navigate(["/ui/nav/setting/asset-add"]);
  }
  onEdit(id){
    this.router.navigate(['/ui/nav/setting/asset-edit',id]);
  }
  confirm(){
    this.service.deleteAssetById(this.assetId).subscribe(
      (data)=>{
        this.basic = false;
        this.refresh(this.currentState)
    })
  }
  onClose(){
    this.basic = false;
  }
  cancel(){
    this.basic = false;
    this.assetId = "";
  }
  onDelete(id){
    this.basic = true;
    this.assetId = id;
  }
  ngOnInit() {
  }

}