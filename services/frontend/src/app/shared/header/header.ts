import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductStatus } from '../../models/product.model';

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
  @Input() statusFilter: ProductStatus | 'all' = 'all';
  @Input() addButtonText = '+ Add product'; // Allow customization of add button text
  @Input() showFilters = true; // Allow hiding filters for supplier page

  @Output() queryChange = new EventEmitter<string>();
  @Output() openFilters = new EventEmitter<void>();
  @Output() filterSelect = new EventEmitter<ProductStatus | 'all'>();
  @Output() addProduct = new EventEmitter<void>();
  @Output() clearSearch = new EventEmitter<void>();
  @Output() deleteSelected = new EventEmitter<void>();

  onInput(v: string) { 
    this.queryChange.emit(v); 
  }

  onFilterClick(status: ProductStatus | 'all') {
    this.filterSelect.emit(status);
  }
}