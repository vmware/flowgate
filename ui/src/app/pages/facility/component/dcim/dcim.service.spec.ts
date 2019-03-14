/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { TestBed, inject } from '@angular/core/testing';

import { DcimService } from './dcim.service';

describe('DcimService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DcimService]
    });
  });

  it('should be created', inject([DcimService], (service: DcimService) => {
    expect(service).toBeTruthy();
  }));
});
