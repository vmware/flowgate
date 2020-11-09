/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component } from '@angular/core';
import { DataServiceService } from './data-service.service';
import { LocalStorage } from './local.storage';

@Component({
    selector: 'my-app',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    providers:[DataServiceService,LocalStorage]
})
export class AppComponent {
    loading:boolean = false;
    ngOnInit(): void {
    
    }
}
