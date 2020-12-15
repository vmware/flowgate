/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule }  from '@angular/router';
import { FacilityComponent } from './facility.component';
import { ModuleWithProviders } from '@angular/core';

export const routes: Routes = [
    {
        path: '',
        component: FacilityComponent,
        children: [
          {path: '', redirectTo: '', pathMatch: 'full'},
          {
            path: 'dcim',
            loadChildren: () => import('app/pages/facility/component/dcim/dcim.module').then(m => m.DcimModule)
          },{
            path: 'cmdb',
            loadChildren: () => import('app/pages/facility/component/cmdb/cmdb.module').then(m => m.CmdbModule)
          }
        ]
      }
];

export const ROUTING: ModuleWithProviders<RouterModule> = RouterModule.forChild(routes);