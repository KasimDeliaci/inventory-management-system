// order-listing.ts
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Order, PurchaseOrderStatus, SalesOrderStatus } from '../../models/order.model';
import { Supplier } from '../../models/supplier.model';
import { Customer } from '../../models/customer.model';
import { Product } from '../../models/product.model';
import { OrderComponent } from '../order/order';

@Component({
  selector: 'app-order-listing',
  standalone: true,
  imports: [CommonModule, OrderComponent],
  templateUrl: './order-listing.html',
  styleUrls: ['./order-listing.scss']
})
export class OrderListingComponent {
  @Input({ required: true }) orders: Order[] = [];
  @Input({ required: true }) suppliers: Supplier[] = [];
  @Input({ required: true }) customers: Customer[] = [];
  @Input({ required: true }) products: Product[] = [];
  @Output() edit = new EventEmitter<Order>();
  @Output() selectionChange = new EventEmitter<void>();
  @Output() statusChange = new EventEmitter<{ order: Order, newStatus: PurchaseOrderStatus | SalesOrderStatus }>();

  allSelected = false;

  toggleAll(checked: boolean) {
    this.allSelected = checked;
    for (const o of this.orders) o.selected = checked;
    this.selectionChange.emit();
  }
}