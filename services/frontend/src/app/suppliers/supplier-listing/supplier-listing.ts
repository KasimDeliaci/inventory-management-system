import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Supplier } from '../../models/supplier.model';
import { SupplierComponent } from '../supplier/supplier';

@Component({
  selector: 'app-supplier-listing',
  standalone: true,
  imports: [CommonModule, SupplierComponent],
  templateUrl: './supplier-listing.html',
  styleUrls: ['./supplier-listing.scss']
})
export class SupplierListingComponent {
  @Input({ required: true }) suppliers: Supplier[] = [];
  @Output() edit = new EventEmitter<Supplier>();
  @Output() selectionChange = new EventEmitter<void>();

  allSelected = false;

  toggleAll(checked: boolean) {
    this.allSelected = checked;
    for (const s of this.suppliers) s.selected = checked;
    this.selectionChange.emit();
  }
}