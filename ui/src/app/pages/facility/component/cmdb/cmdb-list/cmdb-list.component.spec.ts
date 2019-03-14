/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CmdbListComponent } from './cmdb-list.component';

describe('CmdbListComponent', () => {
  let component: CmdbListComponent;
  let fixture: ComponentFixture<CmdbListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CmdbListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CmdbListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
