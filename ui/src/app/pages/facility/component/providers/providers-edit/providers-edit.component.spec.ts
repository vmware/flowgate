import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ProvidersEditComponent } from './providers-edit.component';

describe('ProvidersEditComponent', () => {
  let component: ProvidersEditComponent;
  let fixture: ComponentFixture<ProvidersEditComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ProvidersEditComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProvidersEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
