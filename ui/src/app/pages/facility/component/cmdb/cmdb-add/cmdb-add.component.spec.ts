/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CmdbAddComponent } from './cmdb-add.component';

describe('CmdbAddComponent', () => {
  let component: CmdbAddComponent;
  let fixture: ComponentFixture<CmdbAddComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CmdbAddComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CmdbAddComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
