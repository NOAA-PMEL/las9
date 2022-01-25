import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DatasetOrganizeComponent } from './dataset-organize.component';

describe('DatasetOrganizeComponent', () => {
  let component: DatasetOrganizeComponent;
  let fixture: ComponentFixture<DatasetOrganizeComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DatasetOrganizeComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DatasetOrganizeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
