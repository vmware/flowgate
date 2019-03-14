/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { VmwareComponent } from './vmware.component';

describe('VmwareComponent', () => {
  let component: VmwareComponent;
  let fixture: ComponentFixture<VmwareComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ VmwareComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(VmwareComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
