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
  @Input() loading: boolean = false;

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<Campaign>();
  @Output() delete = new EventEmitter<string>();

  /** Use inject() so it's available for field initializers */
  private fb = inject(FormBuilder);

  form = this.fb.group({
    id: [''],
    name: ['', Validators.required],
    type: ['discount' as CampaignType],
    assignmentType: ['product' as AssignmentType],
    percentage: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    buyQty: [null as number | null],
    getQty: [null as number | null],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    selectedProductId: [''],
    selectedCustomerId: [''],
    isActive: [true],
  });

  ngOnChanges() {
    // Populate form when value changes
    if (this.value) {
      this.form.patchValue({
        id: this.value.id || '',
        name: this.value.name || '',
        type: this.value.type || 'discount',
        assignmentType: this.value.assignmentType || 'product',
        percentage: this.value.percentage || 0,
        buyQty: this.value.buyQty || null,
        getQty: this.value.getQty || null,
        startDate: this.value.startDate || '',
        endDate: this.value.endDate || '',
        selectedProductId: this.value.productIds?.[0] || '',
        selectedCustomerId: this.value.customerIds?.[0] || '',
        isActive: this.value.isActive ?? true,
      });
    }
  }

  // Helper method to get selected product
  getSelectedProduct(): Product | null {
    if (!this.form.value.selectedProductId || !this.products) return null;
    return this.products.find(p => p.id === this.form.value.selectedProductId) || null;
  }

  // Helper method to get selected customer
  getSelectedCustomer(): Customer | null {
    if (!this.form.value.selectedCustomerId || !this.customers) return null;
    return this.customers.find(c => c.id === this.form.value.selectedCustomerId) || null;
  }

  onAssignmentTypeChange(newType: AssignmentType) {
    this.form.controls.assignmentType.setValue(newType);
    // Clear the other selection when switching types
    if (newType === 'product') {
      this.form.controls.selectedCustomerId.setValue('');
    } else {
      this.form.controls.selectedProductId.setValue('');
    }
  }

  onCampaignTypeChange(newType: CampaignType) {
    this.form.controls.type.setValue(newType);
    
    // Handle different campaign types
    if (newType === 'promotion') {
      // For promotion (BXGY), set default values if not set
      if (!this.form.value.buyQty) {
        this.form.controls.buyQty.setValue(2);
      }
      if (!this.form.value.getQty) {
        this.form.controls.getQty.setValue(1);
      }
      // Clear percentage for promotion type
      this.form.controls.percentage.setValue(0);
    } else {
      // For discount types, clear buy/get quantities
      this.form.controls.buyQty.setValue(null);
      this.form.controls.getQty.setValue(null);
    }
  }

  onSave() {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    
    // Convert single selections back to arrays for compatibility
    const productIds = raw.assignmentType === 'product' && raw.selectedProductId 
      ? [raw.selectedProductId] 
      : [];
    const customerIds = raw.assignmentType === 'customer' && raw.selectedCustomerId 
      ? [raw.selectedCustomerId] 
      : [];

    const out: Campaign = {
      ...(this.value ?? { 
        productIds: [], 
        customerIds: [], 
        selected: false 
      }),
      id: raw.id ?? '',
      name: raw.name!,
      description: '', // Always empty since we removed description
      type: raw.type!,
      assignmentType: raw.assignmentType!,
      percentage: Number(raw.percentage ?? 0),
      buyQty: raw.buyQty != null ? Number(raw.buyQty) : null,
      getQty: raw.getQty != null ? Number(raw.getQty) : null,
      startDate: raw.startDate!,
      endDate: raw.endDate!,
      productIds,
      customerIds,
      isActive: raw.isActive ?? true,
    };

    // Validate date range
    if (new Date(out.startDate) >= new Date(out.endDate)) {
      alert('End date must be after start date');
      return;
    }

    // Validate campaign type specific requirements
    if (out.type === 'promotion' && (!out.buyQty || !out.getQty || out.buyQty <= 0 || out.getQty <= 0)) {
      alert('Buy X Get Y campaigns require valid buy and get quantities');
      return;
    }

    if (out.type !== 'promotion' && out.percentage <= 0) {
      alert('Discount campaigns require a valid percentage');
      return;
    }

    // Validate assignment - check if something is selected
    const hasAssignments = (out.assignmentType === 'product' && raw.selectedProductId) ||
                          (out.assignmentType === 'customer' && raw.selectedCustomerId);
    
    if (!hasAssignments) {
      alert(`Please select a ${out.assignmentType} for this campaign`);
      return;
    }

    this.save.emit(out);
  }

  onDelete() {
    if (this.value?.id) this.delete.emit(this.value.id);
  }
}