import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SupplierListing } from './supplier-listing';

describe('SupplierListing', () => {
  let component: SupplierListing;
  let fixture: ComponentFixture<SupplierListing>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SupplierListing]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SupplierListing);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
