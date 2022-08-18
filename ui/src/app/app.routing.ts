/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {CanActivateAuthGuard} from './pages/auth/CanActivateAuthGuard';
export const ROUTES: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    {path:'login',loadChildren: () => import('app/pages/user-login/user-login.module').then(m => m.UserLoginModule)},
    {path:'ui/nav', loadChildren: () => import('app/pages/nav/nav.module').then(m => m.NavModule),canActivate: [CanActivateAuthGuard]}
];

export const ROUTING: ModuleWithProviders<RouterModule> = RouterModule.forRoot(ROUTES,{ useHash: false, relativeLinkResolution: 'legacy' });
