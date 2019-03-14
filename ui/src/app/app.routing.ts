/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { ModuleWithProviders } from '@angular/core/src/metadata/ng_module';
import { Routes, RouterModule } from '@angular/router';
import { UserLoginComponent } from './pages/user-login/user-login.component';
import { NavComponent } from './pages/nav/nav.component';
import {CanActivateAuthGuard} from './pages/auth/CanActivateAuthGuard';
export const ROUTES: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    {path:'login',loadChildren: 'app/pages/user-login/user-login.module#UserLoginModule'},
    {path:'ui/nav', loadChildren: 'app/pages/nav/nav.module#NavModule',canActivate: [CanActivateAuthGuard]}
];

export const ROUTING: ModuleWithProviders = RouterModule.forRoot(ROUTES,{ useHash: false });
