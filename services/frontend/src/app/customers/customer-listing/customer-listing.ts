import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Customer } from '../../models/customer.model';
import { CustomerComponent } from '../customer/customer';

@Component({
  selector: 'app-customer-listing',
  standalone: true,
  imports: [CommonModule, CustomerComponent],
  templateUrl: './customer-listing.html',
  styleUrls: ['./customer-listing.scss']
})
export class CustomerListingComponent {
  @Input({ required: true }) customers: Customer[] = [];
  @Output() edit = new EventEmitter<Customer>();
  @Output() selectionChange = new EventEmitter<void>();

  allSelected = false;

  toggleAll(checked: boolean) {
    this.allSelected = checked;
    for (const c of this.customers) c.selected = checked;
    this.selectionChange.emit();
  }
}