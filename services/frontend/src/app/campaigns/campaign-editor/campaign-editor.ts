// campaign-editor.ts
import { Component, EventEmitter, Input, Output, inject, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormsModule } from '@angular/forms';
import { Campaign, CampaignType, AssignmentType } from '../../models/campaign.model';
import { Product } from '../../models/product.model';
import { Customer } from '../../models/customer.model';

@Component({
  selector: 'app-campaign-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './campaign-editor.html',
  styleUrls: ['./campaign-editor.scss'],
})
export class CampaignEditorComponent implements OnChanges {
  @Input() value: Campaign | null = null;
  @Input() products: Product[] = [];
  @Input() customers: Customer[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<Campaign>();
  @Output() delete = new EventEmitter<string>(); // id

  /** Use inject() so it's available for field initializers */
  private fb = inject(FormBuilder);

  form = this.fb.group({
    id: [''],
    name: ['', Validators.required],
    description: [''],
    type: ['discount' as CampaignType],
    assignmentType: ['product' as AssignmentType],
    percentage: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    productIds: [[] as string[]],
    customerIds: [[] as string[]],
    isActive: [true],
  });



  ngOnChanges() {
    // Populate form when value changes
    if (this.value) {
      this.form.patchValue({
        id: this.value.id || '',
        name: this.value.name || '',
        description: this.value.description || '',
        type: this.value.type || 'discount',
        assignmentType: this.value.assignmentType || 'product',
        percentage: this.value.percentage || 0,
        startDate: this.value.startDate || '',
        endDate: this.value.endDate || '',
        productIds: this.value.productIds || [],
        customerIds: this.value.customerIds || [],
        isActive: this.value.isActive ?? true,
      });
    }
  }

  onAssignmentTypeChange(newType: AssignmentType) {
    this.form.controls.assignmentType.setValue(newType);
    // Clear the other assignment array when switching types
    if (newType === 'product') {
      this.form.controls.customerIds.setValue([]);
    } else {
      this.form.controls.productIds.setValue([]);
    }
  }

  isItemSelected(itemId: string): boolean {
    const assignmentType = this.form.value.assignmentType;
    if (assignmentType === 'product') {
      const productIds = this.form.value.productIds || [];
      return productIds.includes(itemId);
    } else {
      const customerIds = this.form.value.customerIds || [];
      return customerIds.includes(itemId);
    }
  }

  toggleItemSelection(itemId: string, event: any): void {
    const isChecked = event.target.checked;
    const assignmentType = this.form.value.assignmentType;
    
    if (assignmentType === 'product') {
      const currentProductIds = this.form.value.productIds || [];
      if (isChecked) {
        if (!currentProductIds.includes(itemId)) {
          this.form.controls.productIds.setValue([...currentProductIds, itemId]);
        }
      } else {
        this.form.controls.productIds.setValue(
          currentProductIds.filter(id => id !== itemId)
        );
      }
    } else {
      const currentCustomerIds = this.form.value.customerIds || [];
      if (isChecked) {
        if (!currentCustomerIds.includes(itemId)) {
          this.form.controls.customerIds.setValue([...currentCustomerIds, itemId]);
        }
      } else {
        this.form.controls.customerIds.setValue(
          currentCustomerIds.filter(id => id !== itemId)
        );
      }
    }
  }

  onSave() {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const out: Campaign = {
      ...(this.value ?? { 
        productIds: [], 
        customerIds: [], 
        selected: false 
      }),
      id: raw.id ?? '',
      name: raw.name!, // required in form
      description: raw.description ?? '',
      type: raw.type!,
      assignmentType: raw.assignmentType!,
      percentage: Number(raw.percentage ?? 0),
      startDate: raw.startDate!,
      endDate: raw.endDate!,
      productIds: Array.isArray(raw.productIds) ? raw.productIds : [],
      customerIds: Array.isArray(raw.customerIds) ? raw.customerIds : [],
      isActive: raw.isActive ?? true,
    };

    // Validate date range
    if (new Date(out.startDate) >= new Date(out.endDate)) {
      alert('End date must be after start date');
      return;
    }

    // Validate assignment
    const hasAssignments = (out.assignmentType === 'product' && out.productIds.length > 0) ||
                          (out.assignmentType === 'customer' && out.customerIds.length > 0);
    
    if (!hasAssignments) {
      alert(`Please select at least one ${out.assignmentType} for this campaign`);
      return;
    }

    this.save.emit(out);
  }

  onDelete() {
    if (this.value?.id) this.delete.emit(this.value.id);
  }
}