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
  @Input() isDeleting: boolean = false;
  @Input() isSaving: boolean = false;

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

  /** Form with validation for required fields */
  form = this.fb.group({
    id: [''],
    name: ['', Validators.required],
    description: [''],
    price: [null as number | null],
    category: ['', Validators.required], // Required field
    unit: ['', Validators.required], // Required field
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
    // Mark all fields as touched to show validation errors
    this.form.markAllAsTouched();
    
    if (!this.form.valid) {
      // Show specific validation messages
      const errors: string[] = [];
      if (this.form.get('name')?.errors?.['required']) {
        errors.push('Product name is required');
      }
      if (this.form.get('category')?.errors?.['required']) {
        errors.push('Category is required');
      }
      if (this.form.get('unit')?.errors?.['required']) {
        errors.push('Unit of measure is required');
      }
      
      if (errors.length > 0) {
        alert('Please fix the following errors:\n\n' + errors.join('\n'));
      }
      return;
    }

    const raw = this.form.getRawValue();
    const out: Product = {
      ...(this.value ?? { status: 'ok' as const, activeSupplierIds: [] }),
      id: raw.id ?? '',
      name: raw.name!.trim(), // required in form, trim whitespace
      description: raw.description?.trim() || null,
      price: raw.price != null ? Number(raw.price) : null,
      category: raw.category!.trim(), // required in form, trim whitespace
      unit: raw.unit!.trim(), // required in form, trim whitespace
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

  // Helper methods for template
  get isNewProduct(): boolean {
    return !this.value?.id;
  }

  get canDelete(): boolean {
    return !this.isNewProduct && !!this.value?.id;
  }

  // Check if suppliers should be shown
  get shouldShowSuppliers(): boolean {
    // For new products, don't show suppliers until after creation
    // For existing products, always show suppliers
    return !this.isNewProduct && this.suppliers.length > 0;
  }

  // Get message for when suppliers are not shown
  get supplierMessage(): string {
    if (this.isNewProduct) {
      return 'Suppliers can be assigned after creating the product.';
    }
    if (this.suppliers.length === 0) {
      return 'No suppliers available.';
    }
    return '';
  }

  // Field validation helpers
  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field?.invalid && field?.touched);
  }

  getFieldError(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (field?.errors?.['required']) {
      return `${fieldName.charAt(0).toUpperCase() + fieldName.slice(1)} is required`;
    }
    return '';
  }
}