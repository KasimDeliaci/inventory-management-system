import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CustomerListingComponent } from './customer-listing';

describe('CustomerListing', () => {
  let component: CustomerListingComponent;
  let fixture: ComponentFixture<CustomerListingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerListingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CustomerListingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
