/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CmdbEditComponent } from './cmdb-edit.component';

describe('CmdbEditComponent', () => {
  let component: CmdbEditComponent;
  let fixture: ComponentFixture<CmdbEditComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CmdbEditComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CmdbEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
