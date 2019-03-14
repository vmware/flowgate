/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CmdbComponent } from './cmdb.component';

describe('CmdbComponent', () => {
  let component: CmdbComponent;
  let fixture: ComponentFixture<CmdbComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CmdbComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CmdbComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
