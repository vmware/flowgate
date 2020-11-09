/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ClarityModule } from '@clr/angular';
import { AppComponent } from './app.component';
import { ROUTING } from "./app.routing";
import { LocalStorage } from './local.storage';
import { EnvironmentSpecificService } from '../app/core/services/environment-specific.service';
import { UserLoginModule } from './pages/user-login/user-login.module';
import { AuthenticationService } from './pages/auth/authenticationService';
import { CanActivateAuthGuard } from './pages/auth/CanActivateAuthGuard';
import { AuthenticationInterceptor } from './pages/auth/AuthenticationInterceptor';
import { HttpClientModule, HTTP_INTERCEPTORS } from "@angular/common/http";

@NgModule({

    declarations: [
        AppComponent
    ],
    imports: [
        BrowserAnimationsModule,
        BrowserModule,
        FormsModule,
        HttpClientModule,
        ClarityModule,
        UserLoginModule,
        ROUTING
    ],
    providers: [LocalStorage,EnvironmentSpecificService,AuthenticationService,CanActivateAuthGuard,AuthenticationInterceptor
        ,{provide: HTTP_INTERCEPTORS, useClass: AuthenticationInterceptor, multi:true}],
    bootstrap: [AppComponent]
})
export class AppModule {
}
