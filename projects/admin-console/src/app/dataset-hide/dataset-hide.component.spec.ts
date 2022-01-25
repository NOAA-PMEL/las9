import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DatasetHideComponent } from './dataset-hide.component';

describe('DatasetHideComponent', () => {
  let component: DatasetHideComponent;
  let fixture: ComponentFixture<DatasetHideComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DatasetHideComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DatasetHideComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
