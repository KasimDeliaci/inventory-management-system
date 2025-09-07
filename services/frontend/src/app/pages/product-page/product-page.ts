import { Component, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../shared/header/header';
import { SideNav } from '../../shared/side-nav/side-nav';
import { ProductListingComponent } from '../../products/product-listing/product-listing';
import { ProductEditorComponent } from '../../products/product-editor/product-editor';
import { Product, ProductStatus } from '../../models/product.model';
import { Supplier } from '../../models/supplier.model';
import { MockDataService } from './mock-data.service';

@Component({
  selector: 'app-product-page',
  standalone: true,
  imports: [
    CommonModule,
    Header,
    SideNav,
    ProductListingComponent,
    ProductEditorComponent,
  ],
  templateUrl: './product-page.html',
  styleUrls: ['./product-page.scss'],
})
export class ProductPageComponent {
  private mockDataService = inject(MockDataService);

  query = signal('');
  editorOpen = signal(false);
  editing = signal<Product | null>(null);
  
  // Filter states
  filterOpen = signal(false);
  statusFilter = signal<ProductStatus | 'all'>('all');

  private selectionTick = signal(0);
  
  bumpSelection() {
    this.selectionTick.update((n) => n + 1);
  }

  // Initialize with mock data
  private all = signal<Product[]>(this.mockDataService.getProducts());
  suppliers = signal<Supplier[]>(this.mockDataService.getSuppliers());

  readonly products = computed(() => {
    let filtered = this.all();
    
    // Apply status filter
    const statusFilter = this.statusFilter();
    if (statusFilter !== 'all') {
      filtered = filtered.filter(p => p.status === statusFilter);
    }
    
    // Apply search query
    const q = this.query().trim().toLowerCase();
    if (q) {
      filtered = filtered.filter((p) =>
        [
          p.id,
          p.name,
          p.category,
          ...(Array.isArray(p.activeSupplierIds) ? p.activeSupplierIds : []),
        ].some((v) => typeof v === 'string' && v.toLowerCase().includes(q))
      );
    }
    
    return filtered;
  });

  /** Depends on selectionTick so it updates when checkboxes toggle */
  selectedCount = computed(() => {
    this.selectionTick();
    return this.all().filter((p) => p.selected).length;
  });

  // Status change handler
  onStatusChange(event: { product: Product, newStatus: ProductStatus }) {
    this.all.update((list) => 
      list.map((p) => 
        p.id === event.product.id 
          ? { ...p, status: event.newStatus }
          : p
      )
    );
  }

  // Header event handlers
  onAddProduct() {
    this.editing.set({
      id: '',
      name: '',
      category: '',
      unit: '',
      currentStock: 0,
      preferredSupplierId: '',
      activeSupplierIds: [],
      status: 'ok',
      selected: false,
    });
    this.editorOpen.set(true);
  }

  onOpenFilters() {
    this.filterOpen.set(!this.filterOpen());
  }

  onFilterSelect(status: ProductStatus | 'all') {
    this.statusFilter.set(status);
    this.filterOpen.set(false);
  }

  onClear() {
    this.query.set('');
  }

  deleteSelected() {
    const count = this.selectedCount();
    if (count === 0) return;
    
    const message = `Delete ${count} selected product${count > 1 ? 's' : ''}?`;
    if (!confirm(message)) return;
    
    this.all.update((list) => list.filter((p) => !p.selected));
    this.bumpSelection();
  }

  openEditorFor(product: Product) {
    this.editing.set({ ...product });
    this.editorOpen.set(true);
  }

  // Editor event handlers
  handleSave(updated: Product) {
    if (updated.id && this.all().some((p) => p.id === updated.id)) {
      // Update existing product
      this.all.update((list) => 
        list.map((p) => (p.id === updated.id ? { ...p, ...updated } : p))
      );
    } else {
      // Add new product
      const id = updated.id?.trim() || `ID-${String(this.all().length + 1).padStart(3, '0')}`;
      this.all.update((list) => [{ ...updated, id, selected: false }, ...list]);
    }
    this.closeEditor();
  }

  handleDelete(id: string) {
    if (confirm('Are you sure you want to delete this product?')) {
      this.all.update((list) => list.filter((p) => p.id !== id));
      this.closeEditor();
    }
  }

  closeEditor() {
    this.editorOpen.set(false);
    this.editing.set(null);
  }
}