import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DatasetAddComponent } from './dataset-add.component';

describe('DatasetAddComponent', () => {
  let component: DatasetAddComponent;
  let fixture: ComponentFixture<DatasetAddComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DatasetAddComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DatasetAddComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
