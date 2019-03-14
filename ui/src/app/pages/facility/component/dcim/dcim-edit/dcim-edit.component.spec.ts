/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DcimEditComponent } from './dcim-edit.component';

describe('DcimEditComponent', () => {
  let component: DcimEditComponent;
  let fixture: ComponentFixture<DcimEditComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DcimEditComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DcimEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
