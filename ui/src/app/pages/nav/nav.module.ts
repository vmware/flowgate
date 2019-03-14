/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { routing } from './nav.routing';
import { NavComponent } from './nav.component';
import { ClarityModule } from '@clr/angular';
import { HasPrivilegeDirective } from '../auth/HasPrivilegeDirective';
import { PagesModule } from '../pages.module';
@NgModule({
  imports: [
    CommonModule,
    routing,
    ClarityModule,
    PagesModule
  ],
  declarations: [NavComponent]
})
export class NavModule { }
