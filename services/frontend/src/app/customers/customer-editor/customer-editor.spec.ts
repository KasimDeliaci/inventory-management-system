import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CustomerEditor } from './customer-editor';

describe('CustomerEditor', () => {
  let component: CustomerEditor;
  let fixture: ComponentFixture<CustomerEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerEditor]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CustomerEditor);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
