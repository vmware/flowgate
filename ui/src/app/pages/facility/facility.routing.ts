/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule }  from '@angular/router';

import { FacilityComponent } from './facility.component';
import { ModuleWithProviders } from '@angular/core';

// noinspection TypeScriptValidateTypes
export const routes: Routes = [
    {
        path: '',
        component: FacilityComponent,
        children: [
          {path: '', redirectTo: '', pathMatch: 'full'},
          {
            path: 'dcim',
            loadChildren: 'app/pages/facility/component/dcim/dcim.module#DcimModule'
          },{
            path: 'cmdb',
            loadChildren: 'app/pages/facility/component/cmdb/cmdb.module#CmdbModule'
          }
        ]
      }
];

export const routing: ModuleWithProviders = RouterModule.forChild(routes);