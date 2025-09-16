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
  @Input() deleting = false;
  @Input() saving = false; // Add saving state input

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<Supplier>();
  @Output() delete = new EventEmitter<string>(); // id

  /** Use inject() so it's available for field initializers */
  private fb = inject(FormBuilder);

  form = this.fb.group({
    id: [{value: '', disabled: true}], // Make ID field disabled
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
    
    // Handle deleting and saving states
    if (changes['deleting'] || changes['saving']) {
      this.updateFormDisabledState();
    }
  }

  private updateFormDisabledState() {
    const shouldDisable = this.deleting || this.saving;
    
    if (shouldDisable) {
      this.form.get('name')?.disable();
      this.form.get('email')?.disable();
      this.form.get('phone')?.disable();
      this.form.get('city')?.disable();
    } else {
      this.form.get('name')?.enable();
      this.form.get('email')?.enable();
      this.form.get('phone')?.enable();
      this.form.get('city')?.enable();
    }
    // ID field stays disabled always
  }

  private updateForm() {
    if (this.value) {
      console.log('Updating form with value:', this.value);
      this.form.patchValue({
        id: this.value.id || '',
        name: this.value.name || '',
        email: this.value.email || '',
        phone: this.cleanPhoneForEditing(this.value.phone) || '',
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

  // Clean phone number for editing (remove spaces for input field)
  private cleanPhoneForEditing(phone: string): string {
    if (!phone) return phone;
    
    // Remove all spaces, hyphens, and parentheses but keep the + sign
    return phone.replace(/[\s\-\(\)]/g, '');
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
      id: this.isNewSupplier ? '' : (formValue.id?.trim() || ''), // Empty ID for new suppliers
      name: formValue.name?.trim() || '',
      email: formValue.email?.trim() || '',
      phone: formValue.phone?.trim() || '',
      city: formValue.city?.trim() || '',
      selected: this.value?.selected || false
    };

    console.log('Emitting save with:', supplier);
    this.save.emit(supplier);
  }

  onPhoneInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const value = input.value;
    
    // Allow user to type with spaces for better readability
    // The backend formatting will handle the cleanup
    this.form.get('phone')?.setValue(value, { emitEvent: false });
  }

  // Format phone number for display (add spaces for readability)
  formatPhoneForDisplay(phone: string): string {
    if (!phone) return phone;
    
    // Remove all non-digit characters except +
    const cleaned = phone.replace(/[^\d+]/g, '');
    
    // Format Turkish phone numbers: +90 XXX XXX XXXX
    if (cleaned.startsWith('+90') && cleaned.length === 13) {
      return `+90 ${cleaned.slice(3, 6)} ${cleaned.slice(6, 9)} ${cleaned.slice(9)}`;
    }
    
    // Format other international numbers: +XX XXX XXX XXXX
    if (cleaned.startsWith('+') && cleaned.length > 10) {
      const countryCode = cleaned.slice(0, 3);
      const remaining = cleaned.slice(3);
      if (remaining.length === 10) {
        return `${countryCode} ${remaining.slice(0, 3)} ${remaining.slice(3, 6)} ${remaining.slice(6)}`;
      }
    }
    
    // Return original if no pattern matches
    return phone;
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
        return `${fieldName.charAt(0).toUpperCase() + fieldName.slice(1)} is required`;
      }
      if (field.errors['email']) {
        return 'Please enter a valid email address';
      }
    }
    return '';
  }
}