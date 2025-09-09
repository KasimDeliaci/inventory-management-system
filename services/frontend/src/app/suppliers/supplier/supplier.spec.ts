import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Supplier } from './supplier';

describe('Supplier', () => {
  let component: Supplier;
  let fixture: ComponentFixture<Supplier>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Supplier]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Supplier);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
