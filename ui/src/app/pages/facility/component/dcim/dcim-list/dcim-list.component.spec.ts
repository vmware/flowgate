/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DcimListComponent } from './dcim-list.component';

describe('DcimListComponent', () => {
  let component: DcimListComponent;
  let fixture: ComponentFixture<DcimListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DcimListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DcimListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
