/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {Routes, RouterModule}  from '@angular/router';
import {PagesComponent} from './pages.component';
import {ModuleWithProviders} from '@angular/core';


export const routes: Routes = [
  {path: 'login', loadChildren: 'app/pages/user-login/user-login.module#UserLoginModule'}

];

export const routing: ModuleWithProviders = RouterModule.forChild(routes);