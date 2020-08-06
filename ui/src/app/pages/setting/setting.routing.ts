/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule }  from '@angular/router';

import { SettingComponent } from './setting.component';
import { SensorsettingListComponent } from '../setting/component/sensorsetting-list/sensorsetting-list.component';
import { SensorsettingEditComponent } from '../setting/component/sensorsetting-edit/sensorsetting-edit.component';
import { SensorsettingAddComponent } from '../setting/component/sensorsetting-add/sensorsetting-add.component';
import { ModuleWithProviders } from '@angular/core';
import { TriggerJobComponent } from '../setting/component/trigger-job/trigger-job.component';
import { AssetAddComponent } from './component/asset-add/asset-add.component';
import { AssetListComponent } from './component/asset-list/asset-list.component';
import { AssetEditComponent } from './component/asset-edit/asset-edit.component';

// noinspection TypeScriptValidateTypes
export const routes: Routes = [
    {
        path: '',
        component: SettingComponent,
        children: [
          {path: '', redirectTo: 'sensorsetting-list', pathMatch: 'full'},
          {
            path:'sensorsetting-list',
            component:SensorsettingListComponent
          },{
            path:'sensorsetting-edit/:id',
            component:SensorsettingEditComponent
          },{
            path:'sensorsetting-add',
            component:SensorsettingAddComponent
          },{
            path:'system-setting',
            component:TriggerJobComponent
          },{
            path: 'asset-list',
            component: AssetListComponent
          },{
            path: 'asset-add',
            component: AssetAddComponent
          },{
            path: 'asset-edit/:id',
            component: AssetEditComponent
          }
        ]
      }
];

export const routing: ModuleWithProviders = RouterModule.forChild(routes);