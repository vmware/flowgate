
import {map} from 'rxjs/operators';
/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Injectable, OnInit } from '@angular/core';
import {  BehaviorSubject } from 'rxjs';



import { EnvSpecific } from '../models/env-specific';


@Injectable()
export class EnvironmentSpecificService {

  public envSpecific: EnvSpecific;
  public envSpecificNull: EnvSpecific = null;
  private envSpecificSubject: BehaviorSubject<EnvSpecific> = new BehaviorSubject<EnvSpecific>(null);

  constructor() {
  }

  public loadEnvironment() {
     
    //   if (this.envSpecific === null || this.envSpecific === undefined) {
    //     return this.http.get('./assets/url.json').pipe(
    //         map((data) => data)
    //         .toPromise<EnvSpecific>();
    //   }

      return Promise.resolve(this.envSpecificNull);
  }

  public setEnvSpecific(es: EnvSpecific) {

    if (es === null || es === undefined) {
        return;
    }

    this.envSpecific = es;
    if (this.envSpecificSubject) {
        this.envSpecificSubject.next(this.envSpecific);
    }
  }


  public subscribe(caller: any, callback: (caller: any, es: EnvSpecific) => void) {
      this.envSpecificSubject
          .subscribe((es) => {
              if (es === null) {
                  return;
              }
              callback(caller, es);
          });
  }
}
