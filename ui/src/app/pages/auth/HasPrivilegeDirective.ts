/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import {Directive, Input, TemplateRef, ViewContainerRef} from '@angular/core';
import {AuthenticationService} from '../auth/authenticationService';

@Directive({
  selector: '[appHasPrivilege]'
})
export class HasPrivilegeDirective {
  constructor(private templateRef: TemplateRef<any>, private viewContainer: ViewContainerRef, private authenticationService: AuthenticationService) {
  }

  @Input()
  set appHasPrivilege(priviilege: string[]) {
    if (this.authenticationService.hasAuthorities(priviilege)) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    } else {
      this.viewContainer.clear();
    }
  }
}