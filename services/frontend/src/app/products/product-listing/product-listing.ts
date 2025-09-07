import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product, ProductStatus } from '../../models/product.model';
import { ProductComponent } from '../product/product';

@Component({
  selector: 'app-product-listing',
  standalone: true,
  imports: [CommonModule, ProductComponent],
  templateUrl: './product-listing.html',
  styleUrls: ['./product-listing.scss']
})
export class ProductListingComponent {
  @Input({ required: true }) products: Product[] = [];
  @Output() edit = new EventEmitter<Product>();
  @Output() selectionChange = new EventEmitter<void>();
  @Output() statusChange = new EventEmitter<{ product: Product, newStatus: ProductStatus }>();

  allSelected = false;

  toggleAll(checked: boolean) {
    this.allSelected = checked;
    for (const p of this.products) p.selected = checked;
    this.selectionChange.emit();
  }
}