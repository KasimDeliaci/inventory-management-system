import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrderEditor } from './order-editor';

describe('OrderEditor', () => {
  let component: OrderEditor;
  let fixture: ComponentFixture<OrderEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderEditor]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrderEditor);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
