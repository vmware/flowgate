/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import {Router,ActivatedRoute} from '@angular/router';
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

  setInfo(){
    this.info=this.pageSize;
    this.getAssetsDatas(this.currentPage,this.pageSize)
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.getAssetsDatas(this.currentPage,this.pageSize)
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++
      this.getAssetsDatas(this.currentPage,this.pageSize)
    }
  }

  getAssetsDatas(currentPage,pageSize){
    this.assets = [];
    this.service.getAssetsBySource("flowgate", currentPage, pageSize).subscribe(
      (data)=>{
        this.assets =  data['content'];
        this.currentPage = data['number']+1;
        this.totalPage = data['totalPages'];
        if(this.totalPage == 1){
          this.disabled = "disabled";
        }else{
          this.disabled = "";
        }
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
        this.ngOnInit();
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
     this.getAssetsDatas(this.currentPage,this.pageSize);
  }

}