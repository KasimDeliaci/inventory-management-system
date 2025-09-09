import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CustomerEditorComponent } from './customer-editor';

describe('CustomerEditor', () => {
  let component: CustomerEditorComponent;
  let fixture: ComponentFixture<CustomerEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerEditorComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CustomerEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
