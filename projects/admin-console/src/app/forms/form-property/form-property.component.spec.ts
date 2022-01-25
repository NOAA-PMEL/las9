import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FormPropertyComponent } from './form-property.component';

describe('FormPropertyComponent', () => {
  let component: FormPropertyComponent;
  let fixture: ComponentFixture<FormPropertyComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FormPropertyComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FormPropertyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
