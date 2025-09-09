import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductEditorComponent } from './product-editor';

describe('ProductEditor', () => {
  let component: ProductEditorComponent;
  let fixture: ComponentFixture<ProductEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductEditorComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProductEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
