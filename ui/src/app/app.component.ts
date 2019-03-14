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
    constructor(private envSpecificSvc: EnvironmentSpecificService,private router: Router,private data:DataServiceService,private ls:LocalStorage) {
        envSpecificSvc.subscribe(this, this.setLink);
        envSpecificSvc.loadEnvironment().then(es => {
            this.envSpecificSvc.setEnvSpecific(es);
            return this.envSpecificSvc.envSpecific;
        }, error => {
            return null;
        });
    }
    setLink(caller: any, es: EnvSpecific) {
        const thisCaller = caller as AppComponent;
        window.sessionStorage.setItem("auth_url",es.Auth_URL);
        window.sessionStorage.setItem("api_url",es.API_URL);
      }
    
    ngOnInit(): void {
    
    }
}
