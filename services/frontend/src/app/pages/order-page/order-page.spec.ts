import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrderPage } from './order-page';

describe('OrderPage', () => {
  let component: OrderPage;
  let fixture: ComponentFixture<OrderPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrderPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
