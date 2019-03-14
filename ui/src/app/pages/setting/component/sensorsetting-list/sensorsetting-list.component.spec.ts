/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SensorsettingListComponent } from './sensorsetting-list.component';

describe('SensorsettingListComponent', () => {
  let component: SensorsettingListComponent;
  let fixture: ComponentFixture<SensorsettingListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SensorsettingListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SensorsettingListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
