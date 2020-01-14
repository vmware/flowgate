import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ProvidersAddComponent } from './providers-add.component';

describe('ProvidersAddComponent', () => {
  let component: ProvidersAddComponent;
  let fixture: ComponentFixture<ProvidersAddComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ProvidersAddComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProvidersAddComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
