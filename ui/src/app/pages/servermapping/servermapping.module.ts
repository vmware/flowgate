/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClarityModule } from '@clr/angular';
import { FormsModule } from '@angular/forms';
import { ROUTING } from './servermapping.routing'
import { ServermappingComponent } from './servermapping.component';
import { ServermappingService } from './servermapping.service';

@NgModule({
  imports: [
    CommonModule,ROUTING ,ClarityModule,
    FormsModule
  ],
  declarations: [ServermappingComponent],
  providers:[ServermappingService]
})
export class ServermappingModule { 
}
