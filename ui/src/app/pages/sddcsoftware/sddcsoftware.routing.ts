/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule }  from '@angular/router';

import { SddcsoftwareComponent } from './sddcsoftware.component';
import { ModuleWithProviders } from '@angular/core';

// noinspection TypeScriptValidateTypes
export const routes: Routes = [
    {
        path: '',
        component: SddcsoftwareComponent,
        children: [
          {path: '', redirectTo: '', pathMatch: 'full'},
          {
            path: 'vmware',
            loadChildren: 'app/pages/sddcsoftware/component/vmware/vmware.module#VmwareModule'
          }
        ]
      }
];

export const routing: ModuleWithProviders = RouterModule.forChild(routes);