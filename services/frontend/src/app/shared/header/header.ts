import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

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

  @Output() queryChange = new EventEmitter<string>();
  @Output() openFilters = new EventEmitter<void>();
  @Output() addProduct = new EventEmitter<void>();
  @Output() clearSearch = new EventEmitter<void>();
  @Output() deleteSelected = new EventEmitter<void>();

  onInput(v: string) { this.queryChange.emit(v); }
}
