/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { routing } from './setting.routing';
import { ClarityModule } from '@clr/angular';
import { FormsModule } from '@angular/forms';
import { SensorsettingListComponent } from '../setting/component/sensorsetting-list/sensorsetting-list.component';
import { SensorsettingEditComponent } from '../setting/component/sensorsetting-edit/sensorsetting-edit.component';
import { SensorsettingAddComponent } from '../setting/component/sensorsetting-add/sensorsetting-add.component';
import { SettingComponent } from './setting.component';
import { SettingService } from './setting.service';
import { PagesModule } from '../../pages/pages.module';
import { TriggerJobComponent } from '../setting/component/trigger-job/trigger-job.component';
import { AssetChart } from './component/trigger-job/sankey.chart';


@NgModule({
  imports: [
    CommonModule,
    ClarityModule,
    FormsModule,
    PagesModule,
    routing,
    AssetChart
  ],
  schemas: [ CUSTOM_ELEMENTS_SCHEMA ],
  declarations: [SettingComponent,SensorsettingListComponent,SensorsettingEditComponent,SensorsettingAddComponent,TriggerJobComponent,AssetChart],
  providers:[ SettingService ]
})
export class SettingModule { }
