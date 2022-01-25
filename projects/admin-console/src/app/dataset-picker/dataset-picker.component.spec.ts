import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DatasetPickerComponent } from './dataset-picker.component';

describe('DatasetPickerComponent', () => {
  let component: DatasetPickerComponent;
  let fixture: ComponentFixture<DatasetPickerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DatasetPickerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DatasetPickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
