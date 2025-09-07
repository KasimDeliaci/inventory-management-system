import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Customer } from '../../models/customer.model';

@Component({
  selector: 'app-customer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './customer.html',
  styleUrls: ['./customer.scss']
})
export class CustomerComponent {
  @Input({ required: true }) customer!: Customer;
  @Output() toggle = new EventEmitter<boolean>();
  @Output() rowClick = new EventEmitter<Customer>();

  get segmentClass() {
    return {
      sme: this.customer.segment === 'sme',
      individual: this.customer.segment === 'individual',
      institutional: this.customer.segment === 'institutional'
    };
  }
}