import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SupplierComponent } from './supplier';

describe('Supplier', () => {
  let component: SupplierComponent;
  let fixture: ComponentFixture<SupplierComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SupplierComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SupplierComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
