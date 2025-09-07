import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductStatus } from '../../models/product.model';
import { CustomerSegment } from '../../models/customer.model';

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
  
  @Input() addButtonText = '+ Add product'; // Allow customization of add button text
  @Input() showFilters = true; // Allow hiding filters for supplier page
  
  @Output() queryChange = new EventEmitter<string>();
  @Output() openFilters = new EventEmitter<void>();
  
  // Product-specific outputs
  @Output() filterSelect = new EventEmitter<ProductStatus | 'all'>();
  @Output() addProduct = new EventEmitter<void>();
  
  // Customer-specific outputs
  @Output() segmentFilterSelect = new EventEmitter<CustomerSegment | 'all'>();
  @Output() addCustomer = new EventEmitter<void>();
  
  @Output() clearSearch = new EventEmitter<void>();
  @Output() deleteSelected = new EventEmitter<void>();

  // Determine if this is customer page or product page
  get isCustomerPage(): boolean {
    return this.segmentFilter !== undefined;
  }

  get isProductPage(): boolean {
    return this.statusFilter !== undefined;
  }

  get hasActiveFilter(): boolean {
    if (this.isProductPage) {
      return this.statusFilter !== 'all';
    } else if (this.isCustomerPage) {
      return this.segmentFilter !== 'all';
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

  onAddClick() {
    if (this.isCustomerPage) {
      this.addCustomer.emit();
    } else {
      this.addProduct.emit();
    }
  }
}