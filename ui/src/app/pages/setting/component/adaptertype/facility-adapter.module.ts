/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdapterJobCommandModule } from './adapter-job-command.module';

@NgModule({
  imports: [
    CommonModule
  ],
  declarations: []
})
export class FacilityAdapterModule { 
  id:string;
  displayName:string;
  description:string;
  topic:string;
  queueName:string;
  type:string;
  subCategory:string;
  commands:AdapterJobCommandModule[];
  createTime:number;
  serviceKey:string;
}
