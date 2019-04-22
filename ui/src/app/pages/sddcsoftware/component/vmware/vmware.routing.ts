/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule } from '@angular/router';
import { VmwareComponent } from './vmware.component';
import { VmwareConfigAddComponent } from '../vmware/vmware-config-add/vmware-config-add.component';
import { VmwareConfigEditComponent } from '../vmware/vmware-config-edit/vmware-config-edit.component';
import { VmwareConfigListComponent } from '../vmware/vmware-config-list/vmware-config-list.component';

const routes: Routes = [
  {
    path: '',
    component: VmwareComponent,
    children:[
      {path: '', redirectTo: 'vmware-list', pathMatch: 'full'},
      {
      path:'vmware-list',
      component:VmwareConfigListComponent
    },{
      path:'vmware-edit/:id',
      component:VmwareConfigEditComponent
    },{
      path:'vmware-add',
      component:VmwareConfigAddComponent
    }]
  }
];

export const routing = RouterModule.forChild(routes);