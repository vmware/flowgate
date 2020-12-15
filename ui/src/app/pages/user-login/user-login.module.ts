/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ClarityModule } from '@clr/angular';
import { CommonModule } from '@angular/common';
import { ROUTING } from './user-login.routing';
import { UserLoginComponent } from './user-login.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ClarityModule,
    ROUTING
  ],
  declarations: [UserLoginComponent]
  
})
export class UserLoginModule { }
