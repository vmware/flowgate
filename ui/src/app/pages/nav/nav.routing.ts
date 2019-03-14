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
            loadChildren: 'app/pages/user/user.module#UserModule'
          },{
            path: 'sddc',
            loadChildren: 'app/pages/sddcsoftware/sddcsoftware.module#SddcsoftwareModule'
          },{
            path: 'server-mapping',
            loadChildren: 'app/pages/servermapping/servermapping.module#ServermappingModule'
          },{
            path: 'facility',
            loadChildren: 'app/pages/facility/facility.module#FacilityModule'
          },{
            path: 'setting',
            loadChildren: 'app/pages/setting/setting.module#SettingModule'
          }
        ]
      }
];

export const routing: ModuleWithProviders = RouterModule.forChild(routes);