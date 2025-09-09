import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrderEditorComponent } from './order-editor';

describe('OrderEditor', () => {
  let component: OrderEditorComponent;
  let fixture: ComponentFixture<OrderEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderEditorComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrderEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
