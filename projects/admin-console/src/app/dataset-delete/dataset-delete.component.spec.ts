import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DatasetDeleteComponent } from './dataset-delete.component';

describe('DatasetDeleteComponent', () => {
  let component: DatasetDeleteComponent;
  let fixture: ComponentFixture<DatasetDeleteComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DatasetDeleteComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DatasetDeleteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
