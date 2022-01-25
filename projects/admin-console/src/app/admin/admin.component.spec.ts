import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminCompnent } from './admin.component';

describe('AddDataComponent', () => {
  let component: AdminCompnent;
  let fixture: ComponentFixture<AdminCompnent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AdminCompnent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AdminCompnent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
