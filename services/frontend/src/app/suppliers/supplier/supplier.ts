import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Supplier } from '../../models/supplier.model';

@Component({
  selector: 'app-supplier',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './supplier.html',
  styleUrls: ['./supplier.scss']
})
export class SupplierComponent {
  @Input({ required: true }) supplier!: Supplier;
  @Output() toggle = new EventEmitter<boolean>();
  @Output() rowClick = new EventEmitter<Supplier>();
}