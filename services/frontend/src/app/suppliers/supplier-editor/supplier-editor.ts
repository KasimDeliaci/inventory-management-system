import { Component, EventEmitter, Input, Output, inject, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormsModule } from '@angular/forms';
import { Supplier } from '../../models/supplier.model';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-supplier-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './supplier-editor.html',
  styleUrls: ['./supplier-editor.scss'],
})
export class SupplierEditor implements OnChanges {
  @Input() value: Supplier | null = null;
  @Input() associatedProducts: Product[] = [];
  @Input() deleting = false; // Add deleting state input

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<Supplier>();
  @Output() delete = new EventEmitter<string>(); // id

  /** Use inject() so it's available for field initializers */
  private fb = inject(FormBuilder);

  form = this.fb.group({
    id: [''],
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.required],
    city: ['', Validators.required],
  });

  ngOnChanges(changes: SimpleChanges) {
    // Only update form when value input actually changes
    if (changes['value']) {
      this.updateForm();
    }
  }

  private updateForm() {
    if (this.value) {
      console.log('Updating form with value:', this.value);
      this.form.patchValue({
        id: this.value.id || '',
        name: this.value.name || '',
        email: this.value.email || '',
        phone: this.value.phone || '',
        city: this.value.city || '',
      });
    } else {
      // Reset form for new supplier
      console.log('Resetting form for new supplier');
      this.form.reset({
        id: '',
        name: '',
        email: '',
        phone: '',
        city: '',
      });
    }
    
    // Reset validation state when value changes
    this.form.markAsUntouched();
    this.form.markAsPristine();
  }

  onSave() {
    console.log('Form valid:', this.form.valid);
    console.log('Form values:', this.form.getRawValue());

    if (!this.form.valid) {
      this.form.markAllAsTouched();
      console.log('Form is invalid, not saving');
      
      // Show which fields are invalid
      Object.keys(this.form.controls).forEach(key => {
        const control = this.form.get(key);
        if (control?.invalid) {
          console.log(`${key} is invalid:`, control.errors);
        }
      });
      return;
    }

    const formValue = this.form.getRawValue();
    
    // Create the supplier object
    const supplier: Supplier = {
      id: formValue.id?.trim() || '',
      name: formValue.name?.trim() || '',
      email: formValue.email?.trim() || '',
      phone: formValue.phone?.trim() || '',
      city: formValue.city?.trim() || '',
      selected: this.value?.selected || false
    };

    console.log('Emitting save with:', supplier);
    this.save.emit(supplier);
  }

  onDelete() {
    if (this.value?.id && !this.deleting) {
      this.delete.emit(this.value.id);
    }
  }

  // Getter methods for template
  get isNewSupplier(): boolean {
    return !this.value?.id;
  }

  // Get products where this supplier is preferred
  get preferredProducts(): Product[] {
    if (!this.value?.id) return [];
    return this.associatedProducts.filter((p) => p.preferredSupplierId === this.value!.id);
  }

  // Get products where this supplier is active but not preferred
  get activeProducts(): Product[] {
    if (!this.value?.id) return [];
    return this.associatedProducts.filter(
      (p) =>
        p.activeSupplierIds.includes(this.value!.id) &&
        p.preferredSupplierId !== this.value!.id
    );
  }

  // Helper method to get field error messages
  getFieldError(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        return `${fieldName} is required`;
      }
      if (field.errors['email']) {
        return 'Please enter a valid email address';
      }
    }
    return '';
  }
}