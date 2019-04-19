/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { DataServiceService } from './data-service.service';
import { LocalStorage } from './local.storage';
import {Http,RequestOptions } from '@angular/http'
import { EnvironmentSpecificService } from '../app/core/services/environment-specific.service';
import { EnvSpecific } from '../app/core/models/env-specific';
import { environment } from '../environments/environment.prod';

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
