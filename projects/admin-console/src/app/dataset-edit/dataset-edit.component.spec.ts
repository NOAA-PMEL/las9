import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DatasetEditComponent } from './dataset-edit.component';

describe('DatasetEditComponent', () => {
  let component: DatasetEditComponent;
  let fixture: ComponentFixture<DatasetEditComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DatasetEditComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DatasetEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
