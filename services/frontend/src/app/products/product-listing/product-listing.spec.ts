import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductListing } from './product-listing';

describe('ProductListing', () => {
  let component: ProductListing;
  let fixture: ComponentFixture<ProductListing>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductListing]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProductListing);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
