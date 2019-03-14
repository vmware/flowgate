/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { ClarityModule } from '@clr/angular';
import { AppComponent } from './app.component';
import { ROUTING } from "./app.routing";
import { UserListComponent } from './pages/user/component/user-list/user-list.component';
import { UserEditComponent } from './pages/user/component/user-edit/user-edit.component';
import { NavComponent } from './pages/nav/nav.component';
import { LocalStorage } from './local.storage';
import { UserLoginComponent } from './pages/user-login/user-login.component';
import { UserComponent } from './pages/user/user.component';
import { PagesComponent } from './pages/pages.component';
import { EnvironmentSpecificService } from '../app/core/services/environment-specific.service';
import { UserLoginModule } from './pages/user-login/user-login.module';
import { AuthenticationService } from './pages/auth/authenticationService';
import { CanActivateAuthGuard } from './pages/auth/CanActivateAuthGuard';
import { AuthenticationInterceptor } from './pages/auth/AuthenticationInterceptor';
import { HttpInterceptorService } from './pages/auth/HttpInterceptorService';
import { Http, XHRBackend, RequestOptions }    from '@angular/http';
export function interceptorFactory(xhrBackend: XHRBackend, requestOptions: RequestOptions){
    let service = new HttpInterceptorService(xhrBackend, requestOptions);
    return service;
  }

@NgModule({

    declarations: [
        AppComponent
 
    ],
    imports: [
        BrowserAnimationsModule,
        BrowserModule,
        FormsModule,
        HttpModule,
        ClarityModule,
        UserLoginModule,
        ROUTING
    ],
    providers: [LocalStorage,EnvironmentSpecificService,AuthenticationService,CanActivateAuthGuard,HttpInterceptorService,
        {
          provide: Http,
          useFactory: interceptorFactory,
          deps: [XHRBackend, RequestOptions]
        }],
    bootstrap: [AppComponent]
})
export class AppModule {
}
