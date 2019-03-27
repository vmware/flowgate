/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FacilityComponent } from './facility.component';
import { routing } from './facility.routing';
import { CmdbComponent } from './component/cmdb/cmdb.component';
import { CmdbAddComponent } from './component/cmdb/cmdb-add/cmdb-add.component';
import { CmdbEditComponent } from './component/cmdb/cmdb-edit/cmdb-edit.component';
import { CmdbListComponent } from './component/cmdb/cmdb-list/cmdb-list.component';
@NgModule({
  imports: [
    CommonModule,
    routing
  ],
  declarations: [FacilityComponent]
})
export class FacilityModule { 
  public id;
  public type;
  public name;
  public description;
  public userName;
  public password;
  public serverURL;
  public verifyCert;
  public advanceSetting;
  public integrationStatus;
}
