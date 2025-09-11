// updated-customer-page.ts
import { Component, computed, signal, inject, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../shared/header/header';
import { SideNav } from '../../shared/side-nav/side-nav';
import { CustomerListingComponent } from '../../customers/customer-listing/customer-listing';
import { CustomerEditorComponent } from '../../customers/customer-editor/customer-editor';
import { Customer, CustomerSegment } from '../../models/customer.model';
import { DataService } from '../data.service'; // Import the new DataService

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
export class CustomerPageComponent implements OnInit {
  private dataService = inject(DataService); // Use DataService instead of MockDataService
  private destroyRef = inject(DestroyRef);

  query = signal('');
  editorOpen = signal(false);
  editing = signal<Customer | null>(null);
  
  // Filter states
  filterOpen = signal(false);
  segmentFilter = signal<CustomerSegment | 'all'>('all');
  
  // Loading state
  loading = signal(false);

  private selectionTick = signal(0);
  
  bumpSelection() {
    this.selectionTick.update((n) => n + 1);
  }

  // Initialize with signals
  private all = signal<Customer[]>([]);

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

  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    this.loading.set(true);
    
    // Load customers from backend
    const customersSubscription = this.dataService.getCustomers().subscribe({
      next: (customers) => {
        this.all.set(customers);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading customers:', err);
        this.loading.set(false);
      },
    });

    // Cleanup subscriptions
    this.destroyRef.onDestroy(() => {
      customersSubscription.unsubscribe();
    });
  }

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
    // Validate required fields
    if (!updated.name?.trim() || !updated.email?.trim() || !updated.phone?.trim() || !updated.city?.trim()) {
      console.error('Missing required fields');
      return;
    }

    // Ensure we have clean data
    const cleanUpdated: Customer = {
      id: updated.id?.trim() || '',
      name: updated.name.trim(),
      segment: updated.segment,
      email: updated.email.trim(),
      phone: updated.phone.trim(),
      city: updated.city.trim(),
      selected: updated.selected || false,
    };

    // Check if this is an update (existing customer with ID) or new customer
    const isUpdate = cleanUpdated.id && this.all().some((c) => c.id === cleanUpdated.id);
    
    if (isUpdate) {
      // Update existing customer
      this.all.update((list) => 
        list.map((c) => (c.id === cleanUpdated.id ? { ...c, ...cleanUpdated } : c))
      );
    } else {
      // Add new customer - generate ID if empty
      const maxIdNumber = Math.max(
        ...this.all().map(c => {
          const match = c.id.match(/CUST-(\d+)/);
          return match ? parseInt(match[1]) : 0;
        }),
        500 // Start from 501 to match backend pattern
      );
      const id = cleanUpdated.id || `CUST-${String(maxIdNumber + 1).padStart(3, '0')}`;
      this.all.update((list) => [{ ...cleanUpdated, id, selected: false }, ...list]);
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

  // Refresh data method
  refreshData() {
    this.loadData();
  }
}