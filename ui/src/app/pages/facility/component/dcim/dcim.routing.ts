/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule } from '@angular/router';
import { DcimComponent } from './dcim.component';
import { DcimListComponent } from '../dcim/dcim-list/dcim-list.component';
import { DcimAddComponent } from '../dcim/dcim-add/dcim-add.component';
import { DcimEditComponent } from '../dcim/dcim-edit/dcim-edit.component';
const routes: Routes = [
    {
      path: '',
      component: DcimComponent,
      children:[
        {path: '', redirectTo: 'dcim-list', pathMatch: 'full'},
        {
        path:'dcim-list',
        component:DcimListComponent
      },{
        path:'dcim-edit/:id',
        component:DcimEditComponent
      },{
        path:'dcim-add',
        component:DcimAddComponent
      }]
    }
  ];
  
  export const routing = RouterModule.forChild(routes);