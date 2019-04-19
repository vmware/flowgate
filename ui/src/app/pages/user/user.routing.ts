/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Routes, RouterModule } from '@angular/router';
import { UserComponent } from './user.component';
import { UserListComponent } from '../user/component/user-list/user-list.component';
import { UserEditComponent } from '../user/component/user-edit/user-edit.component';
import { UserAddComponent } from '../user/component/user-add/user-add.component';
import { RoleListComponent } from './component/role-list/role-list.component';
import { UserProfileComponent } from './component/user-profile/user-profile.component';

const routes: Routes = [
  {
    path: '',
    component: UserComponent,
    //redirectTo: 'pages/standard-procedure-definition/list',
    //pathMatch: 'full',
    children:[
      {path: '', redirectTo: 'user-list', pathMatch: 'full'},

      {
      path:'user-list',
      component:UserListComponent
    },{
      path:'user-edit/:id',
      component:UserEditComponent
    },{
      path:'user-add',
      component:UserAddComponent
    },{
      path:'user-profile',
      component:UserProfileComponent
    },{
      path:'role-list',
      component:RoleListComponent
    }]
  }
];

export const routing = RouterModule.forChild(routes);
