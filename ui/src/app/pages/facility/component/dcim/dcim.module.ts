/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { routing } from './dcim.routing';
import { ClarityModule } from '@clr/angular';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DcimComponent } from './dcim.component';
import { DcimListComponent } from '../dcim/dcim-list/dcim-list.component';
import { DcimAddComponent } from '../dcim/dcim-add/dcim-add.component';
import { DcimEditComponent } from '../dcim/dcim-edit/dcim-edit.component';
import { DcimService } from './dcim.service';
import { PagesModule } from '../../../pages.module';
@NgModule({
  imports: [
    CommonModule,
    routing,
    ClarityModule,
    FormsModule,
    PagesModule,
    ReactiveFormsModule,
  ],
  declarations: [DcimComponent,DcimListComponent,DcimAddComponent,DcimEditComponent],
  providers:[DcimService]
})
export class DcimModule {}
