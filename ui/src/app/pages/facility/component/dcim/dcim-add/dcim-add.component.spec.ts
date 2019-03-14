/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DcimAddComponent } from './dcim-add.component';

describe('DcimAddComponent', () => {
  let component: DcimAddComponent;
  let fixture: ComponentFixture<DcimAddComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DcimAddComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DcimAddComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
