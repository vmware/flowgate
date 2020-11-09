/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {Routes, RouterModule}  from '@angular/router';
import {ModuleWithProviders} from '@angular/core';


export const routes: Routes = [
  {path: 'login', loadChildren: () => import('app/pages/user-login/user-login.module').then(m => m.UserLoginModule)}

];

export const ROUTING: ModuleWithProviders<RouterModule> = RouterModule.forChild(routes);