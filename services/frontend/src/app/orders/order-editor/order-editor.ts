// order-editor.ts
import { Component, EventEmitter, Input, Output, inject, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Order, OrderType, PurchaseOrderStatus, SalesOrderStatus, isPurchaseOrder } from '../../models/order.model';
import { Supplier } from '../../models/supplier.model';
import { Customer } from '../../models/customer.model';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-order-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './order-editor.html',
  styleUrls: ['./order-editor.scss'],
})
export class OrderEditorComponent implements OnChanges {
  @Input() value: Order | null = null;
  @Input() suppliers: Supplier[] = [];
  @Input() customers: Customer[] = [];
  @Input() products: Product[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<Order>();
  @Output() delete = new EventEmitter<string>();

  private fb = inject(FormBuilder);

  form = this.fb.group({
    id: [''],
    type: ['purchase' as OrderType, Validators.required],
    supplierId: [''],
    customerId: [''],
    productId: [''],
    orderDate: ['', Validators.required],
    deliveryDate: [''],
    quantity: [0, [Validators.required, Validators.min(1)]],
    totalPrice: [0, [Validators.required, Validators.min(0)]],
    status: ['placed' as PurchaseOrderStatus | SalesOrderStatus, Validators.required],
    notes: ['']
  });

  // Track if order date picker is open
  orderDateOpen = false;
  deliveryDateOpen = false;

  ngOnChanges() {
    if (this.value) {
      this.form.patchValue({
        id: this.value.id || '',
        type: this.value.type,
        supplierId: this.value.supplierId || '',
        customerId: this.value.customerId || '',
        productId: this.value.productId || '',
        orderDate: this.value.orderDate,
        deliveryDate: this.value.deliveryDate || '',
        quantity: this.value.quantity,
        totalPrice: this.value.totalPrice,
        status: this.value.status,
        notes: this.value.notes || ''
      });
    }
  }

  get isNewOrder(): boolean {
    return !this.value?.id;
  }

  get isPurchaseOrder(): boolean {
    return this.form.value.type === 'purchase';
  }

  get purchaseStatuses(): PurchaseOrderStatus[] {
    return ['placed', 'in_transit', 'received', 'canceled'];
  }

  get salesStatuses(): SalesOrderStatus[] {
    return ['pending', 'allocated', 'in_transit', 'delivered', 'canceled'];
  }

  get currentStatuses(): (PurchaseOrderStatus | SalesOrderStatus)[] {
    return this.isPurchaseOrder ? this.purchaseStatuses : this.salesStatuses;
  }

  onTypeChange() {
    // Reset supplier/customer when type changes
    this.form.patchValue({
      supplierId: '',
      customerId: '',
      status: this.isPurchaseOrder ? 'placed' : 'pending'
    });
  }

  onSave() {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const out: Order = {
      ...(this.value ?? {}),
      id: raw.id ?? '',
      type: raw.type!,
      supplierId: raw.type === 'purchase' ? raw.supplierId || undefined : undefined,
      customerId: raw.type === 'sales' ? raw.customerId || undefined : undefined,
      productId: raw.productId || undefined,
      orderDate: raw.orderDate!,
      deliveryDate: raw.deliveryDate || undefined,
      quantity: raw.quantity!,
      totalPrice: raw.totalPrice!,
      status: raw.status!,
      notes: raw.notes || undefined
    };

    this.save.emit(out);
  }

  onDelete() {
    if (this.value?.id) {
      this.delete.emit(this.value.id);
    }
  }

  toggleOrderDatePicker() {
    this.orderDateOpen = !this.orderDateOpen;
    this.deliveryDateOpen = false;
  }

  toggleDeliveryDatePicker() {
    this.deliveryDateOpen = !this.deliveryDateOpen;
    this.orderDateOpen = false;
  }

  setOrderDate(date: string) {
    this.form.patchValue({ orderDate: date });
    this.orderDateOpen = false;
  }

  setDeliveryDate(date: string) {
    this.form.patchValue({ deliveryDate: date });
    this.deliveryDateOpen = false;
  }

  generateDateOptions(): string[] {
    const dates = [];
    const today = new Date();
    
    // Generate dates from 30 days ago to 60 days in the future
    for (let i = -30; i <= 60; i++) {
      const date = new Date(today);
      date.setDate(today.getDate() + i);
      dates.push(date.toISOString().split('T')[0]);
    }
    
    return dates;
  }

  formatDateDisplay(dateString: string): string {
    if (!dateString) return 'Select date';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }
}