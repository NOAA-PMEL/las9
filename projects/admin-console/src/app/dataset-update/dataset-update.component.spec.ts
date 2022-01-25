import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DatasetUpdateComponent } from './dataset-update.component';

describe('DatasetUpdateComponent', () => {
  let component: DatasetUpdateComponent;
  let fixture: ComponentFixture<DatasetUpdateComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DatasetUpdateComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DatasetUpdateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
