import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SiteEditComponent } from './app-site-edit.component';

describe('AppSiteEditComponent', () => {
  let component: SiteEditComponent;
  let fixture: ComponentFixture<SiteEditComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SiteEditComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SiteEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
