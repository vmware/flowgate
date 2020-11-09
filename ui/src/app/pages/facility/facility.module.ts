/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FacilityComponent } from './facility.component';
import { ROUTING } from './facility.routing';
@NgModule({
  imports: [
    CommonModule,
    ROUTING
  ],
  declarations: [FacilityComponent]
})
export class FacilityModule { 
  id:string;
  type:string;
  name:string;
  description:string;
  userName:string;
  password:string;
  serverURL:string;
  verifyCert:any;
  advanceSetting:any;
  integrationStatus:any;
  subCategory:string;
}
