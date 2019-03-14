/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { ClarityModule } from '@clr/angular';
import { CommonModule } from '@angular/common';
import { routing } from './user-login.routing';
import { UserLoginComponent } from './user-login.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    HttpModule,
    ClarityModule,
    routing
  ],
  declarations: [UserLoginComponent]
  
})
export class UserLoginModule { }
