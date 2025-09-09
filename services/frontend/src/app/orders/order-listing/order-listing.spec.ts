import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrderListing } from './order-listing';

describe('OrderListing', () => {
  let component: OrderListing;
  let fixture: ComponentFixture<OrderListing>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderListing]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrderListing);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
