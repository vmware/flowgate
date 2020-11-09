/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit } from '@angular/core';
import {Router,ActivatedRoute} from '@angular/router';
import { SettingService } from '../../setting.service';
@Component({
  selector: 'app-sensorsetting-add',
  templateUrl: './sensorsetting-add.component.html',
  styleUrls: ['./sensorsetting-add.component.scss']
})
export class SensorsettingAddComponent implements OnInit {

  constructor(private service:SettingService,private router:Router) { }

  sensorsetting = {
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
      this.service.postsensorsetting(this.sensorsetting.type,this.sensorsetting.minNum,this.sensorsetting.maxNum,this.sensorsetting.minValue,this.sensorsetting.maxValue).subscribe(
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
  }
}
