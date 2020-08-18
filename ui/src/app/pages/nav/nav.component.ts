/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit ,Input} from '@angular/core';
import {ActivatedRoute,Router} from "@angular/router";
import { AuthenticationService } from '../auth/authenticationService';
@Component({
  selector: 'app-nav',
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss']
})
export class NavComponent implements OnInit {

  username:any;
  id:any;
  demoCollapsible:boolean = true;

  user:string[] = ["deleteUser","readUserByPage","readUserByID","readUsers","createRole","readRole","readRolesByPage","deleteRole","updateRole"];
  userManagement:string[] = ["deleteUser","readUserByPage","readUserByID","readUsers"];
  roleManagement:string[] = ["createRole","readRole","readRolesByPage","deleteRole","updateRole"];
  sddc:string[] = ["createSddcSoftwareConfig","deleteSddcSoftwareConfig","updateSddcSoftwareConfig",
  "readSddcSoftwareConfigByID","readSddcSoftwareConfigByVRO","readSddcSoftwareConfigByVC","readSddcSoftwareConfigByUserAndPage","readVROsSddcSoftwareConfigByUser","readSddcSoftwareConfigByTypeAndUser"];
  servermapping:string []= ["updateServerMapping","readMappingsByVROIDAndPage","readMappingsByVCIDAndPage","readSddcSoftwareConfigByTypeAndUser"];
  facility:string[] = ["createFacilitySoftwareConfig","readFacilityByType","readFacilityByPage"];
  
  setting:string[] = ["createSensorSetting","readSensorSettingsByPage","updateSensorSetting","deleteSensorSetting","startFullMappingAggregation","generateServerPDUMapping","readUnMappedServers"];
  sensorsetting:string[] = ["createSensorSetting","readSensorSettingsByPage","updateSensorSetting","deleteSensorSetting"];
  systemSetting:string[] = ["startFullMappingAggregation","generateServerPDUMapping","readUnMappedServers"];
  assetmanagement:string [] = ["createAnAsset", "updateAsset", "deleteAsset", "readAsset", "readAssetBySource"];
  adaptermanagement:string [] = ["createFacilityAdapter","updateFacilityAdapter","readAnFacilityAdapterById","deleteAnFacilityAdapterById","readFacilityAdaptersByPage"];
  constructor(private activedRoute:ActivatedRoute,private router: Router,private auth:AuthenticationService) { }
  logout(){
    this.auth.logout();
    
  }
  userprofile(){
    this.router.navigate(["/ui/nav/user/user-profile"]);
  }
  
  ngOnInit() {
      this.username = this.auth.getUsername();
  }
}
