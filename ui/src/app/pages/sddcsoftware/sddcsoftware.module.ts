/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SddcsoftwareComponent } from './sddcsoftware.component';
import { routing } from './sddcsoftware.routing';
import { VmwareComponent } from './component/vmware/vmware.component';

@NgModule({
  imports: [
    CommonModule,
    routing,
    
  ],
  declarations: [SddcsoftwareComponent]
})
export class SddcsoftwareModule { }
