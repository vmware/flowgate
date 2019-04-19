/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule } from '@angular/router';
import { CmdbComponent } from './cmdb.component';
import { CmdbListComponent } from './cmdb-list/cmdb-list.component';
import { CmdbEditComponent } from './cmdb-edit/cmdb-edit.component';
import { CmdbAddComponent } from './cmdb-add/cmdb-add.component';

const routes: Routes = [
    {
      path: '',
      component: CmdbComponent,
      children:[
        {path: '', redirectTo: 'cmdb-list', pathMatch: 'full'},
        {
        path:'cmdb-list',
        component:CmdbListComponent
      },{
        path:'cmdb-edit/:id',
        component:CmdbEditComponent
      },{
        path:'cmdb-add',
        component:CmdbAddComponent
      }]
    }
  ];
  
  export const routing = RouterModule.forChild(routes);