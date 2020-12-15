/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule }  from '@angular/router';

import { ServermappingComponent } from './servermapping.component';
import { ModuleWithProviders } from '@angular/core';

// noinspection TypeScriptValidateTypes
export const routes: Routes = [
    { path: '', redirectTo: 'server-mapping', pathMatch: 'full' },
    {path: 'server-mapping',component: ServermappingComponent}
];

export const ROUTING: ModuleWithProviders<RouterModule> = RouterModule.forChild(routes);