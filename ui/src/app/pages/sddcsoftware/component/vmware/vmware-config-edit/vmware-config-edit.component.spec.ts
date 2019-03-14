/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { VmwareConfigEditComponent } from './vmware-config-edit.component';

describe('VmwareConfigEditComponent', () => {
  let component: VmwareConfigEditComponent;
  let fixture: ComponentFixture<VmwareConfigEditComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ VmwareConfigEditComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(VmwareConfigEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
