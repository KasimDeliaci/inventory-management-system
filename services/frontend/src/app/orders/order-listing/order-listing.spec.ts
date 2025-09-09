import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrderListingComponent } from './order-listing';

describe('OrderListing', () => {
  let component: OrderListingComponent;
  let fixture: ComponentFixture<OrderListingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderListingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrderListingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
