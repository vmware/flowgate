/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule }  from '@angular/router';

import { UserLoginComponent } from './user-login.component';
import { ModuleWithProviders } from '@angular/core';

// noinspection TypeScriptValidateTypes
export const routes: Routes = [
  {
    path: '',
    component: UserLoginComponent
  }
];

export const ROUTING: ModuleWithProviders<RouterModule> = RouterModule.forChild(routes);
