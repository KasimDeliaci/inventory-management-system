// order-row.ts
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Order, PurchaseOrderStatus, SalesOrderStatus, isPurchaseOrder, isSalesOrder, getStatusColor, PURCHASE_STATUS_ORDER, SALES_STATUS_ORDER } from '../../models/order.model';
import { Supplier } from '../../models/supplier.model';
import { Customer } from '../../models/customer.model';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-order',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order.html',
  styleUrls: ['./order.scss']
})
export class OrderComponent {
  @Input({ required: true }) order!: Order;
  @Input({ required: true }) suppliers: Supplier[] = [];
  @Input({ required: true }) customers: Customer[] = [];
  @Input({ required: true }) products: Product[] = [];
  @Output() toggle = new EventEmitter<boolean>();
  @Output() rowClick = new EventEmitter<Order>();
  @Output() statusChange = new EventEmitter<{ order: Order, newStatus: PurchaseOrderStatus | SalesOrderStatus }>();

  get statusColor() {
    return getStatusColor(this.order.status);
  }

  get typeDisplay() {
    return this.order.type === 'purchase' ? 'P' : 'S';
  }

  get typeClass() {
    return this.order.type === 'purchase' ? 'purchase' : 'sales';
  }

  get supplierCustomerDisplay() {
    if (this.order.type === 'purchase' && this.order.supplierId) {
      const supplier = this.suppliers.find(s => s.id === this.order.supplierId);
      return supplier ? supplier.name : this.order.supplierId;
    } else if (this.order.type === 'sales' && this.order.customerId) {
      const customer = this.customers.find(c => c.id === this.order.customerId);
      return customer ? customer.name : this.order.customerId;
    }
    return '';
  }

  get formattedOrderDate() {
    return new Date(this.order.orderDate).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  get formattedDeliveryDate() {
    if (!this.order.deliveryDate) return '';
    return new Date(this.order.deliveryDate).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  get formattedTotalPrice() {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(this.order.totalPrice);
  }

  get statusLabel() {
    return this.order.status.replace('_', ' ').toUpperCase();
  }

  onStatusClick(event: Event) {
    event.stopPropagation();
    
    let newStatus: PurchaseOrderStatus | SalesOrderStatus;
    
    if (isPurchaseOrder(this.order)) {
      const currentIndex = PURCHASE_STATUS_ORDER.indexOf(this.order.status as PurchaseOrderStatus);
      const nextIndex = (currentIndex + 1) % PURCHASE_STATUS_ORDER.length;
      newStatus = PURCHASE_STATUS_ORDER[nextIndex];
    } else if (isSalesOrder(this.order)) {
      const currentIndex = SALES_STATUS_ORDER.indexOf(this.order.status as SalesOrderStatus);
      const nextIndex = (currentIndex + 1) % SALES_STATUS_ORDER.length;
      newStatus = SALES_STATUS_ORDER[nextIndex];
    } else {
      return;
    }
    
    this.statusChange.emit({ order: this.order, newStatus });
  }
}