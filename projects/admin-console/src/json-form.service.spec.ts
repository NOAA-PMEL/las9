import { TestBed } from '@angular/core/testing';

import { JsonFormService } from './json-form.service';

describe('JsonFormService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: JsonFormService = TestBed.get(JsonFormService);
    expect(service).toBeTruthy();
  });
});
