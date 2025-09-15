import { Component, EventEmitter, Input, Output, inject, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormsModule } from '@angular/forms';
import { Product } from '../../models/product.model';
import { Supplier } from '../../models/supplier.model';

@Component({
  selector: 'app-product-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './product-editor.html',
  styleUrls: ['./product-editor.scss'],
})
export class ProductEditorComponent implements OnChanges {
  @Input() value: Product | null = null;
  @Input() suppliers: Supplier[] = [];
  @Input() isDeleting: boolean = false; // New input for delete loading state

  /** Returns suppliers sorted: preferred first, then actives, then others */
  get sortedSuppliers(): Supplier[] {
    if (!this.suppliers || this.suppliers.length === 0) return [];
    
    const preferredId = this.form.value.preferredSupplierId || null;
    const activeIds = this.form.value.activeSupplierIds || [];
    
    const preferred = preferredId ? this.suppliers.filter(s => s.id === preferredId) : [];
    const actives = this.suppliers.filter(s => s.id !== preferredId && activeIds.includes(s.id));
    const others = this.suppliers.filter(s => s.id !== preferredId && !activeIds.includes(s.id));
    
    return [...preferred, ...actives, ...others];
  }

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<Product>();
  @Output() delete = new EventEmitter<string>(); // id

  /** Use inject() so it's available for field initializers */
  private fb = inject(FormBuilder);

  /** Allow nulls for numeric fields */
  form = this.fb.group({
    id: [''],
    name: ['', Validators.required],
    description: [''],
    price: [null as number | null],
    category: [''],
    unit: [''],
    safetyStock: [null as number | null],
    reorderPoint: [null as number | null],
    currentStock: [null as number | null],

    preferredSupplierId: [''],
    activeSupplierIds: [[] as string[]],
  });

  ngOnChanges(changes: SimpleChanges) {
    // Check if the value input has changed
    if (changes['value'] && this.value) {
      // Always update the form when value changes, regardless of whether it's the same object reference
      this.form.patchValue({
        id: this.value.id || '',
        name: this.value.name || '',
        description: this.value.description || '',
        price: this.value.price,
        category: this.value.category || '',
        unit: this.value.unit || '',
        safetyStock: this.value.safetyStock,
        reorderPoint: this.value.reorderPoint,
        currentStock: this.value.currentStock,
        preferredSupplierId: this.value.preferredSupplierId || '',
        activeSupplierIds: this.value.activeSupplierIds || [],
      }, { emitEvent: false }); // Prevent unnecessary form events
    }
  }

  onSave() {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const out: Product = {
      ...(this.value ?? { status: 'ok' as const, activeSupplierIds: [] }),
      id: raw.id ?? '',
      name: raw.name!, // required in form
      description: raw.description ?? '',
      price: raw.price != null ? Number(raw.price) : null,
      category: raw.category ?? '',
      unit: raw.unit ?? '',
      safetyStock: raw.safetyStock != null ? Number(raw.safetyStock) : null,
      reorderPoint: raw.reorderPoint != null ? Number(raw.reorderPoint) : null,
      currentStock: raw.currentStock != null ? Number(raw.currentStock) : null,

      preferredSupplierId: raw.preferredSupplierId || null,
      activeSupplierIds: Array.isArray(raw.activeSupplierIds) ? raw.activeSupplierIds : [],
    };

    this.save.emit(out);
  }

  onDelete() {
    if (this.value?.id) this.delete.emit(this.value.id);
  }

  // Supplier management methods
  isActiveSupplier(supplierId: string): boolean {
    const activeIds = this.form.value.activeSupplierIds || [];
    return activeIds.includes(supplierId);
  }

  toggleActiveSupplier(supplierId: string, event: any): void {
    const isChecked = event.target.checked;
    const currentActive = this.form.value.activeSupplierIds || [];
    
    if (isChecked) {
      if (!currentActive.includes(supplierId)) {
        this.form.controls.activeSupplierIds.setValue([...currentActive, supplierId]);
      }
    } else {
      this.form.controls.activeSupplierIds.setValue(
        currentActive.filter(id => id !== supplierId)
      );
      // If removing preferred supplier from active, clear preferred
      if (supplierId === this.form.value.preferredSupplierId) {
        this.form.controls.preferredSupplierId.setValue('');
      }
    }
  }

  setPreferredSupplier(supplierId: string): void {
    this.form.controls.preferredSupplierId.setValue(supplierId);
    // Ensure preferred supplier is also in active suppliers
    const currentActive = this.form.value.activeSupplierIds || [];
    if (!currentActive.includes(supplierId)) {
      this.form.controls.activeSupplierIds.setValue([...currentActive, supplierId]);
    }
  }
}