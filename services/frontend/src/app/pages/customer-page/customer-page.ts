import { Component, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../shared/header/header';
import { SideNav } from '../../shared/side-nav/side-nav';
import { CustomerListingComponent } from '../../customers/customer-listing/customer-listing';
import { CustomerEditorComponent } from '../../customers/customer-editor/customer-editor';
import { Customer, CustomerSegment } from '../../models/customer.model';
import { MockDataService } from '../mock-data.service';

@Component({
  selector: 'app-customer-page',
  standalone: true,
  imports: [
    CommonModule,
    Header,
    SideNav,
    CustomerListingComponent,
    CustomerEditorComponent,
  ],
  templateUrl: './customer-page.html',
  styleUrls: ['./customer-page.scss'],
})
export class CustomerPageComponent {
  private mockDataService = inject(MockDataService);

  query = signal('');
  editorOpen = signal(false);
  editing = signal<Customer | null>(null);
  
  // Filter states
  filterOpen = signal(false);
  segmentFilter = signal<CustomerSegment | 'all'>('all');

  private selectionTick = signal(0);
  
  bumpSelection() {
    this.selectionTick.update((n) => n + 1);
  }

  // Initialize with mock data
  private all = signal<Customer[]>(this.mockDataService.getCustomers());

  readonly customers = computed(() => {
    let filtered = this.all();
    
    // Apply segment filter
    const segmentFilter = this.segmentFilter();
    if (segmentFilter !== 'all') {
      filtered = filtered.filter(c => c.segment === segmentFilter);
    }
    
    // Apply search query
    const q = this.query().trim().toLowerCase();
    if (q) {
      filtered = filtered.filter((c) =>
        [
          c.id,
          c.name,
          c.segment,
          c.email,
          c.phone,
          c.city,
        ].some((v) => typeof v === 'string' && v.toLowerCase().includes(q))
      );
    }
    
    return filtered;
  });

  /** Depends on selectionTick so it updates when checkboxes toggle */
  selectedCount = computed(() => {
    this.selectionTick();
    return this.all().filter((c) => c.selected).length;
  });

  // Header event handlers
  onAddCustomer() {
    this.editing.set({
      id: '',
      name: '',
      segment: 'individual',
      email: '',
      phone: '',
      city: '',
      selected: false,
    });
    this.editorOpen.set(true);
  }

  onOpenFilters() {
    this.filterOpen.set(!this.filterOpen());
  }

  onFilterSelect(segment: CustomerSegment | 'all') {
    this.segmentFilter.set(segment);
    this.filterOpen.set(false);
  }

  onClear() {
    this.query.set('');
  }

  deleteSelected() {
    const count = this.selectedCount();
    if (count === 0) return;
    
    const message = `Delete ${count} selected customer${count > 1 ? 's' : ''}?`;
    if (!confirm(message)) return;
    
    this.all.update((list) => list.filter((c) => !c.selected));
    this.bumpSelection();
  }

  openEditorFor(customer: Customer) {
    this.editing.set({ ...customer });
    this.editorOpen.set(true);
  }

  // Editor event handlers
  handleSave(updated: Customer) {
    if (updated.id && this.all().some((c) => c.id === updated.id)) {
      // Update existing customer
      this.all.update((list) => 
        list.map((c) => (c.id === updated.id ? { ...c, ...updated } : c))
      );
    } else {
      // Add new customer
      const id = updated.id?.trim() || `CUS-${String(this.all().length + 1).padStart(3, '0')}`;
      this.all.update((list) => [{ ...updated, id, selected: false }, ...list]);
    }
    this.closeEditor();
  }

  handleDelete(id: string) {
    if (confirm('Are you sure you want to delete this customer?')) {
      this.all.update((list) => list.filter((c) => c.id !== id));
      this.closeEditor();
    }
  }

  closeEditor() {
    this.editorOpen.set(false);
    this.editing.set(null);
  }
}