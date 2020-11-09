/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import { SettingService } from '../../setting.service';
import {Router,ActivatedRoute} from '@angular/router';
@Component({
  selector: 'app-sensorsetting-edit',
  templateUrl: './sensorsetting-edit.component.html',
  styleUrls: ['./sensorsetting-edit.component.scss']
})
export class SensorsettingEditComponent implements OnInit {

  constructor(private service:SettingService,private router:Router,private activedRoute:ActivatedRoute) { }

  sensorsetting = {
    id:"",
    type:"",
    minNum:"",
    maxNum:"",
    minValue:"",
    maxValue:""
  }
  modalIsOpen = false;
  operationTip = "";
  save(){
    if(this.sensorsetting.type == ""){
      this.modalIsOpen = true;
      this.operationTip = "please select a sensor type.";
      
    }
    else if((this.sensorsetting.minNum != "" && this.sensorsetting.maxNum != "") || (this.sensorsetting.minValue != "" && this.sensorsetting.maxValue !="")){
      this.modalIsOpen = false;
      this.operationTip = "";
      this.service.updatesensorsetting(this.sensorsetting.id,this.sensorsetting.type,this.sensorsetting.minNum,this.sensorsetting.maxNum,this.sensorsetting.minValue,this.sensorsetting.maxValue).subscribe(
        (data)=>{
          this.router.navigate(["/ui/nav/setting"]);
        
        }
      )
    }else{
      this.modalIsOpen = true;
      this.operationTip = "If the sensor data is numeric then please set the MinNum and MaxNum, else please set the MaxValue and MinValue fields.";
     
    }
  }
  confirm(){
    this.modalIsOpen = false;
    this.operationTip = "";
  }
  cancel(){
    this.router.navigate(["/ui/nav/setting"]);
  }

  ngOnInit() {
    this.sensorsetting.id = this.activedRoute.snapshot.params['id'];
    if(this.sensorsetting.id != null && this.sensorsetting.id != ""){
      this.service.getsensorsetting(this.sensorsetting.id).subscribe(
        (data)=>{
          //TODO need a sensorsetting model
          //this.sensorsetting = data;
        }
      )
    }
  }

}
