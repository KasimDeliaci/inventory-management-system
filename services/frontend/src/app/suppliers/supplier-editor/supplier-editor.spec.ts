import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SupplierEditor } from './supplier-editor';

describe('SupplierEditor', () => {
  let component: SupplierEditor;
  let fixture: ComponentFixture<SupplierEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SupplierEditor]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SupplierEditor);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
