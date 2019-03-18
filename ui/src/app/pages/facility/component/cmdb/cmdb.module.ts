/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PagesModule } from '../../../pages.module';
import { routing } from './cmdb.routing';
import { ClarityModule } from '@clr/angular';
import { ReactiveFormsModule,FormsModule } from '@angular/forms';
import { CmdbComponent } from './cmdb.component';
import { CmdbAddComponent } from './cmdb-add/cmdb-add.component';
import { CmdbEditComponent } from './cmdb-edit/cmdb-edit.component';
import { CmdbListComponent } from './cmdb-list/cmdb-list.component';
import { DcimService } from '../dcim/dcim.service';
@NgModule({
  imports: [
    CommonModule,
    routing,
    ClarityModule,
    FormsModule,
    ReactiveFormsModule,
    PagesModule
  ],
  declarations: [CmdbComponent,CmdbAddComponent,CmdbEditComponent,CmdbListComponent],
  providers:[DcimService]
})
export class CmdbModule { }
