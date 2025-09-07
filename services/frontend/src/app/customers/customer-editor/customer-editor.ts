import { Component, EventEmitter, Input, Output, inject, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormsModule } from '@angular/forms';
import { Customer, CustomerSegment } from '../../models/customer.model';

@Component({
  selector: 'app-customer-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './customer-editor.html',
  styleUrls: ['./customer-editor.scss'],
})
export class CustomerEditorComponent implements OnChanges {
  @Input() value: Customer | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<Customer>();
  @Output() delete = new EventEmitter<string>(); // id

  /** Use inject() so it's available for field initializers */
  private fb = inject(FormBuilder);

  form = this.fb.group({
    id: [''],
    name: ['', Validators.required],
    segment: ['individual' as CustomerSegment],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    city: [''],
  });

  ngOnChanges() {
    // Populate form when value changes
    if (this.value) {
      this.form.patchValue({
        id: this.value.id || '',
        name: this.value.name || '',
        segment: this.value.segment || 'individual',
        email: this.value.email || '',
        phone: this.value.phone || '',
        city: this.value.city || '',
      });
    }
  }

  onSave() {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const out: Customer = {
      ...(this.value ?? { selected: false }),
      id: raw.id ?? '',
      name: raw.name!, // required in form
      segment: raw.segment! as CustomerSegment,
      email: raw.email!,
      phone: raw.phone ?? '',
      city: raw.city ?? '',
    };

    this.save.emit(out);
  }

  onDelete() {
    if (this.value?.id) this.delete.emit(this.value.id);
  }
}