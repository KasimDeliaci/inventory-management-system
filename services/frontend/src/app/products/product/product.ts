import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Product, ProductStatus } from '../../models/product.model';

@Component({
  selector: 'app-product',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product.html',
  styleUrls: ['./product.scss']
})
export class ProductComponent {
  @Input({ required: true }) product!: Product;
  @Output() toggle = new EventEmitter<boolean>();
  @Output() rowClick = new EventEmitter<Product>();
  @Output() statusChange = new EventEmitter<{ product: Product, newStatus: ProductStatus }>();

  get statusClass() {
    return {
      ok: this.product.status === 'ok',
      warning: this.product.status === 'warning',
      critical: this.product.status === 'critical'
    };
  }

  // Method to get other suppliers (excluding preferred)
  get otherSupplierIds() {
    return this.product.activeSupplierIds?.filter(
      id => id !== this.product.preferredSupplierId
    ) || [];
  }

  // Method to format supplier display
  get supplierDisplay() {
    const preferred = this.product.preferredSupplierId;
    const others = this.otherSupplierIds;
    
    if (others.length === 0) {
      return preferred;
    }
    return `${preferred}, ${others.join(', ')}`;
  }

  onStatusClick(event: Event) {
    event.stopPropagation(); // Prevent row click
    
    // Cycle through statuses: ok -> warning -> critical -> ok
    let newStatus: ProductStatus;
    switch (this.product.status) {
      case 'ok':
        newStatus = 'warning';
        break;
      case 'warning':
        newStatus = 'critical';
        break;
      case 'critical':
        newStatus = 'ok';
        break;
      default:
        newStatus = 'ok';
    }
    
    // Emit the status change event
    this.statusChange.emit({ product: this.product, newStatus });
  }
}