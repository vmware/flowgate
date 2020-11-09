/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule }  from '@angular/router';

import { NavComponent } from './nav.component';
import { ModuleWithProviders } from '@angular/core';
export const routes: Routes = [
    {
        path: '',
        component: NavComponent,
        children: [
          {path: '', redirectTo: 'server-mapping', pathMatch: 'full'},
          {
            path: 'user',
            loadChildren: () => import('app/pages/user/user.module').then(m => m.UserModule)
          },{
            path: 'sddc',
            loadChildren: () => import('app/pages/sddcsoftware/sddcsoftware.module').then(m => m.SddcsoftwareModule)
          },{
            path: 'server-mapping',
            loadChildren: () => import('app/pages/servermapping/servermapping.module').then(m => m.ServermappingModule)
          },{
            path: 'facility',
            loadChildren: () => import('app/pages/facility/facility.module').then(m => m.FacilityModule)
          },{
            path: 'setting',
            loadChildren: () => import('app/pages/setting/setting.module').then(m => m.SettingModule)
          }
        ]
      }
];

export const ROUTING: ModuleWithProviders<RouterModule> = RouterModule.forChild(routes);