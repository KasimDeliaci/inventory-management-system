import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductStatus } from '../../models/product.model';
import { CustomerSegment } from '../../models/customer.model';
import { OrderType, PurchaseOrderStatus, SalesOrderStatus } from '../../models/order.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.html',
  styleUrls: ['./header.scss']
})
export class Header {
  @Input() title = 'Product Listing';
  @Input() query = '';
  @Input() deletableCount = 0;
  @Input() filterOpen = false;
  
  // Product-specific inputs
  @Input() statusFilter?: ProductStatus | 'all';
  
  // Customer-specific inputs
  @Input() segmentFilter?: CustomerSegment | 'all';
  
  // Order-specific inputs
  @Input() typeFilter?: OrderType | 'all';
  @Input() orderStatusFilter?: PurchaseOrderStatus | SalesOrderStatus | 'all';
  
  @Input() addButtonText = '+ Add product';
  @Input() showFilters = true;
  @Input() showAddButton = true;
  @Input() customActionButtons: { text: string; action: string; }[] = [];
  
  @Output() queryChange = new EventEmitter<string>();
  @Output() openFilters = new EventEmitter<void>();
  
  // Product-specific outputs
  @Output() filterSelect = new EventEmitter<ProductStatus | 'all'>();
  @Output() addProduct = new EventEmitter<void>();
  
  // Customer-specific outputs
  @Output() segmentFilterSelect = new EventEmitter<CustomerSegment | 'all'>();
  @Output() addCustomer = new EventEmitter<void>();
  
  // Order-specific outputs
  @Output() typeFilterSelect = new EventEmitter<OrderType | 'all'>();
  @Output() statusFilterSelect = new EventEmitter<PurchaseOrderStatus | SalesOrderStatus | 'all'>();
  @Output() customAction = new EventEmitter<string>();
  
  @Output() clearSearch = new EventEmitter<void>();
  @Output() deleteSelected = new EventEmitter<void>();

  // Determine page type
  get isCustomerPage(): boolean {
    return this.segmentFilter !== undefined;
  }

  get isProductPage(): boolean {
    return this.statusFilter !== undefined;
  }

  get isOrderPage(): boolean {
    return this.typeFilter !== undefined;
  }

  get hasActiveFilter(): boolean {
    if (this.isProductPage) {
      return this.statusFilter !== 'all';
    } else if (this.isCustomerPage) {
      return this.segmentFilter !== 'all';
    } else if (this.isOrderPage) {
      return this.typeFilter !== 'all' || this.orderStatusFilter !== 'all';
    }
    return false;
  }

  onInput(v: string) {
    this.queryChange.emit(v);
  }

  onFilterClick(filter: string) {
    if (this.isProductPage) {
      this.filterSelect.emit(filter as ProductStatus | 'all');
    } else if (this.isCustomerPage) {
      this.segmentFilterSelect.emit(filter as CustomerSegment | 'all');
    }
  }

  onOrderTypeFilterClick(type: OrderType | 'all') {
    this.typeFilterSelect.emit(type);
  }

  onOrderStatusFilterClick(status: PurchaseOrderStatus | SalesOrderStatus | 'all') {
    this.statusFilterSelect.emit(status);
  }

  onAddClick() {
    if (this.isCustomerPage) {
      this.addCustomer.emit();
    } else {
      this.addProduct.emit();
    }
  }

  onCustomActionClick(action: string) {
    this.customAction.emit(action);
  }
}