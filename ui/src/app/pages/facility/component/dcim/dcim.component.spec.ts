/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DcimComponent } from './dcim.component';

describe('DcimComponent', () => {
  let component: DcimComponent;
  let fixture: ComponentFixture<DcimComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DcimComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DcimComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
