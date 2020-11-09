/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PagesComponent } from './pages.component';
import { ROUTING } from './pages.routing';
import { HasPrivilegeDirective } from './auth/HasPrivilegeDirective';


@NgModule({
  imports: [
    CommonModule,
    ROUTING
  ],
  declarations: [PagesComponent,HasPrivilegeDirective],
  exports:[HasPrivilegeDirective]
})
export class PagesModule { }
