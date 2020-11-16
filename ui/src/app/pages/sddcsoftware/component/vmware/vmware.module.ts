/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { routing } from './vmware.routing';
import { ClarityModule } from '@clr/angular';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { VmwareConfigAddComponent } from '../vmware/vmware-config-add/vmware-config-add.component';
import { VmwareConfigEditComponent } from '../vmware/vmware-config-edit/vmware-config-edit.component';
import { VmwareConfigListComponent } from '../vmware/vmware-config-list/vmware-config-list.component';
import { VmwareComponent } from './vmware.component';
import { VmwareService } from './vmware.service';
import { PagesModule } from '../../../pages.module';
@NgModule({
  imports: [
    CommonModule,
    routing,
    ClarityModule,
    PagesModule,
    FormsModule,
    ReactiveFormsModule
  ],
  declarations: [VmwareComponent,VmwareConfigAddComponent,VmwareConfigEditComponent,VmwareConfigListComponent],
  providers:[VmwareService]
})
export class VmwareModule { }
