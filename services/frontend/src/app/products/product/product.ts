import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Product, ProductStatus } from '../../models/product.model';

@Component({
  selector: 'app-product',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product.html',
  styleUrls: ['./product.scss'],
})
export class ProductComponent {
  @Input({ required: true }) product!: Product;
  @Output() toggle = new EventEmitter<boolean>();
  @Output() rowClick = new EventEmitter<Product>();
  @Output() statusChange = new EventEmitter<{ product: Product; newStatus: ProductStatus }>();

  showTooltip = false;
  tooltipTimeout?: number;

  get statusClass() {
    return {
      ok: this.product.status === 'ok',
      warning: this.product.status === 'warning',
      critical: this.product.status === 'critical',
    };
  }

  // Method to get other suppliers (excluding preferred)
  get otherSupplierIds() {
    return (
      this.product.activeSupplierIds?.filter((id) => id !== this.product.preferredSupplierId) || []
    );
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

  // Get tooltip message based on status
  get tooltipMessage() {
    switch (this.product.status) {
      case 'ok':
        return 'Sufficient stock.';
      case 'warning':
        return 'Reorder advised.';
      case 'critical':
        return 'Critical – restock!';
      default:
        return '';
    }
  }

  onStatusClick(event: Event) {
    event.stopPropagation(); // Prevent row click

    // Toggle tooltip visibility
    if (this.showTooltip) {
      this.hideTooltip();
    } else {
      this.showTooltip = true;

      // Clear any existing timeout
      if (this.tooltipTimeout) {
        clearTimeout(this.tooltipTimeout);
      }

      // Auto-hide tooltip after 3 seconds
      this.tooltipTimeout = window.setTimeout(() => {
        this.showTooltip = false;
      }, 5000);
    }
  }

  hideTooltip() {
    this.showTooltip = false;
    if (this.tooltipTimeout) {
      clearTimeout(this.tooltipTimeout);
    }
  }

  ngOnDestroy() {
    if (this.tooltipTimeout) {
      clearTimeout(this.tooltipTimeout);
    }
  }
}
