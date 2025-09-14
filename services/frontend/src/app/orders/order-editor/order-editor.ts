// order-editor.ts
import { Component, EventEmitter, Input, Output, inject, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { Order, OrderType, PurchaseOrderStatus, SalesOrderStatus, isPurchaseOrder } from '../../models/order.model';
import { Supplier } from '../../models/supplier.model';
import { Customer } from '../../models/customer.model';
import { Product } from '../../models/product.model';

// Add interface for order items
interface OrderItem {
  productId: string;
  productName?: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  discountPercentage?: number;
  discountAmount?: number;
  campaignId?: string;
}

// Enhanced Order interface to include items
interface OrderWithItems extends Order {
  items?: OrderItem[];
}

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

  // Main order form - removed productId and quantity since they're now in items
  form = this.fb.group({
    id: [''],
    type: ['purchase' as OrderType, Validators.required],
    supplierId: [''],
    customerId: [''],
    orderDate: ['', Validators.required],
    deliveryDate: [''],
    totalPrice: [0, [Validators.required, Validators.min(0)]],
    status: ['placed' as PurchaseOrderStatus | SalesOrderStatus, Validators.required],
    notes: ['']
  });

  // Form for editing individual order items
  itemForm: FormGroup = this.fb.group({
    productId: ['', Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    unitPrice: [0, [Validators.required, Validators.min(0)]],
    discountPercentage: [0, [Validators.min(0), Validators.max(100)]]
  });

  // Track which item is being edited (-1 means none)
  editingItemIndex: number = -1;

  // Track if date pickers are open
  orderDateOpen = false;
  deliveryDateOpen = false;

  // Local copy of order items
  private _orderItems: OrderItem[] = [];

  ngOnChanges() {
    if (this.value) {
      // Load the main order data
      this.form.patchValue({
        id: this.value.id || '',
        type: this.value.type,
        supplierId: this.value.supplierId || '',
        customerId: this.value.customerId || '',
        orderDate: this.value.orderDate,
        deliveryDate: this.value.deliveryDate || '',
        totalPrice: this.value.totalPrice,
        status: this.value.status,
        notes: this.value.notes || ''
      });

      // Load order items
      const orderWithItems = this.value as OrderWithItems;
      this._orderItems = orderWithItems.items ? [...orderWithItems.items] : [];
      
      // Update calculated totals
      this.updateCalculatedValues();
    } else {
      this._orderItems = [];
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

  get hasOrderItems(): boolean {
    return this._orderItems.length > 0;
  }

  get orderItems(): OrderItem[] {
    return this._orderItems;
  }

  onTypeChange() {
    // Reset supplier/customer when type changes
    this.form.patchValue({
      supplierId: '',
      customerId: '',
      status: this.isPurchaseOrder ? 'placed' : 'pending'
    });
  }

  // Calculate total quantity across all items
  getTotalQuantity(): number {
    return this._orderItems.reduce((total, item) => total + item.quantity, 0);
  }

  // Calculate total price from items
  getCalculatedTotal(): number {
    return this._orderItems.reduce((total, item) => total + item.lineTotal, 0);
  }

  // Update form values based on items
  updateCalculatedValues() {
    const calculatedTotal = this.getCalculatedTotal();
    // Only update if significantly different to avoid overwriting manual entries
    if (Math.abs(calculatedTotal - (this.form.value.totalPrice || 0)) > 0.01) {
      this.form.patchValue({
        totalPrice: calculatedTotal
      });
    }
  }

  // Add new order item
  addOrderItem() {
    this.editingItemIndex = this._orderItems.length;
    this.itemForm.reset({
      productId: '',
      quantity: 1,
      unitPrice: 0,
      discountPercentage: 0
    });
  }

  // Edit existing order item
  editOrderItem(index: number) {
    if (index >= 0 && index < this._orderItems.length) {
      this.editingItemIndex = index;
      const item = this._orderItems[index];
      this.itemForm.patchValue({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        discountPercentage: item.discountPercentage || 0
      });
    }
  }

  // Remove order item
  removeOrderItem(index: number) {
    if (index >= 0 && index < this._orderItems.length) {
      this._orderItems.splice(index, 1);
      this.updateCalculatedValues();
    }
  }

  // Save the current item being edited
  saveOrderItem() {
    if (!this.itemForm.valid) {
      this.itemForm.markAllAsTouched();
      return;
    }

    const formValue = this.itemForm.getRawValue();
    const quantity = formValue.quantity!;
    const unitPrice = formValue.unitPrice!;
    const discountPercentage = formValue.discountPercentage || 0;
    
    // Calculate discount amount and line total
    const discountAmount = (unitPrice * quantity * discountPercentage) / 100;
    const lineTotal = (unitPrice * quantity) - discountAmount;

    const orderItem: OrderItem = {
      productId: formValue.productId!,
      productName: this.getProductName(formValue.productId!),
      quantity: quantity,
      unitPrice: unitPrice,
      lineTotal: lineTotal,
      discountPercentage: discountPercentage > 0 ? discountPercentage : undefined,
      discountAmount: discountAmount > 0 ? discountAmount : undefined
    };

    // Add or update item
    if (this.editingItemIndex === this._orderItems.length) {
      // Adding new item
      this._orderItems.push(orderItem);
    } else {
      // Updating existing item
      this._orderItems[this.editingItemIndex] = orderItem;
    }

    this.cancelEditItem();
    this.updateCalculatedValues();
  }

  // Cancel item editing
  cancelEditItem() {
    this.editingItemIndex = -1;
    this.itemForm.reset();
  }

  onSave() {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }

    // Validate that we have at least one order item
    if (this._orderItems.length === 0) {
      alert('Please add at least one product to the order');
      return;
    }

    const raw = this.form.getRawValue();
    const out: OrderWithItems = {
      ...(this.value ?? {}),
      id: raw.id ?? '',
      type: raw.type!,
      supplierId: raw.type === 'purchase' ? raw.supplierId || undefined : undefined,
      customerId: raw.type === 'sales' ? raw.customerId || undefined : undefined,
      orderDate: raw.orderDate!,
      deliveryDate: raw.deliveryDate || undefined,
      quantity: this.getTotalQuantity(), // Calculate from items
      totalPrice: raw.totalPrice!,
      status: raw.status!,
      notes: raw.notes || undefined,
      items: [...this._orderItems] // Include the items array
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

  getProductName(productId: string): string {
    const product = this.products.find(p => p.id === productId);
    return product ? product.name : `Product ${productId}`;
  }
}