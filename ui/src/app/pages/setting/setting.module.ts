/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ROUTING } from './setting.routing';
import { ClarityModule } from '@clr/angular';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SensorsettingListComponent } from '../setting/component/sensorsetting-list/sensorsetting-list.component';
import { SensorsettingEditComponent } from '../setting/component/sensorsetting-edit/sensorsetting-edit.component';
import { SensorsettingAddComponent } from '../setting/component/sensorsetting-add/sensorsetting-add.component';
import { SettingComponent } from './setting.component';
import { SettingService } from './setting.service';
import { PagesModule } from '../../pages/pages.module';
import { TriggerJobComponent } from '../setting/component/trigger-job/trigger-job.component';
//import { AssetChart } from './component/trigger-job/sankey.chart';
import { FileUploadModule } from 'ng2-file-upload';
import { AssetAddComponent } from './component/asset-add/asset-add.component';
import { AssetListComponent } from './component/asset-list/asset-list.component';
import { AssetEditComponent } from './component/asset-edit/asset-edit.component';
import { FacilityAdapterListComponent } from './component/adaptertype/adaptertype-list/facility-adapter-list.component';
import { FacilityAdapterService } from './component/adaptertype/facility-adapter.service';
import { AssetChart } from './component/trigger-job/sankey.chart';

@NgModule({
  imports: [
    CommonModule,
    ClarityModule,
    FormsModule,
    ReactiveFormsModule,
    PagesModule,
    ROUTING,
    //AssetChart,
    FileUploadModule
  ],
  schemas: [ CUSTOM_ELEMENTS_SCHEMA],
  declarations: [AssetChart,SettingComponent,SensorsettingListComponent,SensorsettingEditComponent,SensorsettingAddComponent,TriggerJobComponent,AssetAddComponent,AssetListComponent,AssetEditComponent,FacilityAdapterListComponent],
  providers:[ SettingService,FacilityAdapterService]
})
export class SettingModule { }
