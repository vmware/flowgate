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
            path:'job-trigger',
            component:TriggerJobComponent
          }
        ]
      }
];

export const routing: ModuleWithProviders = RouterModule.forChild(routes);