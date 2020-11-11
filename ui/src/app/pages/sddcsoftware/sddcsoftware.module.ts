/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SddcsoftwareComponent } from './sddcsoftware.component';
import { ROUTING } from './sddcsoftware.routing';

@NgModule({
  imports: [
    CommonModule,
    ROUTING,
    
  ],
  declarations: [SddcsoftwareComponent]
})
export class SddcsoftwareModule {
  id:string;
  name:string;
  description:string;
  userName:string;
  password:string;
  serverURL:string;
  type:string;
  userId:string;
  verifyCert:boolean;
  integrationStatus:any;
 }
