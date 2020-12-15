/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { routing } from './user.routing';
import { ClarityModule } from '@clr/angular';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { UserService } from './user.service';
import { UserComponent } from './user.component';
import { UserListComponent } from './component/user-list/user-list.component';
import { UserEditComponent } from './component/user-edit/user-edit.component';
import { UserAddComponent } from './component/user-add/user-add.component';
import { AuthorityService } from './authority.service';
import { RoleListComponent } from './component/role-list/role-list.component';
import { RoleService } from './role.service';
import { PagesModule } from '../pages.module';
import { UserProfileComponent } from './component/user-profile/user-profile.component';


@NgModule({
  imports: [
    CommonModule,
    ClarityModule,
    FormsModule,
    ReactiveFormsModule, 
    PagesModule,
    routing
  ],
  declarations: [UserComponent,UserListComponent,UserEditComponent, UserAddComponent, RoleListComponent, UserProfileComponent],
  providers: [UserService,AuthorityService,RoleService,FormBuilder]
})
export class UserModule { }