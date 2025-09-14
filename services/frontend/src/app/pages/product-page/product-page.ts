import { Component, computed, signal, inject, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../shared/header/header';
import { SideNav } from '../../shared/side-nav/side-nav';
import { ProductListingComponent } from '../../products/product-listing/product-listing';
import { ProductEditorComponent } from '../../products/product-editor/product-editor';
import { Product, ProductStatus } from '../../models/product.model';
import { Supplier } from '../../models/supplier.model';
import { ProductService } from '../product.service';
import { SupplierService } from '../supplier.service';

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
export class ProductPageComponent implements OnInit {
  private productService = inject(ProductService);
  private supplierService = inject(SupplierService);
  private destroyRef = inject(DestroyRef);

  query = signal('');
  editorOpen = signal(false);
  editing = signal<Product | null>(null);
  
  // Filter states
  filterOpen = signal(false);
  statusFilter = signal<ProductStatus | 'all'>('all');
  
  // Loading state
  loading = signal(false);
  detailsLoading = signal(false);

  private selectionTick = signal(0);
  
  bumpSelection() {
    this.selectionTick.update((n) => n + 1);
  }

  // Initialize with signals
  private all = signal<Product[]>([]);
  suppliers = signal<Supplier[]>([]);

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

  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    this.loading.set(true);
    
    // Load products from backend
    const productsSubscription = this.productService.getProducts().subscribe({
      next: (products) => {
        this.all.set(products);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading products:', err);
        this.loading.set(false);
      },
    });

    // Cleanup subscriptions when component is destroyed
    this.destroyRef.onDestroy(() => {
      productsSubscription.unsubscribe();
    });
  }

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
    // Set loading state
    this.detailsLoading.set(true);
    this.editorOpen.set(true);

    const detailsSubscription = this.productService.getProductById(product.id).subscribe({
      next: (detailedProduct) => {
        if (detailedProduct) {
          // Use detailed product data with all fields populated
          this.editing.set(detailedProduct);
        } else {
          // Fallback to the basic product data from the list
          console.warn('Could not load detailed product data, using basic info');
          this.editing.set({ ...product });
        }
        this.detailsLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading product details:', err);
        // Fallback to basic product data
        this.editing.set({ ...product });
        this.detailsLoading.set(false);
      },
    });

    const suppliersSubscription = this.supplierService.getSuppliers().subscribe({
      next: (suppliers) => {
        this.suppliers.set(suppliers);
      },
      error: (err) => {
        console.error('Error loading suppliers:', err);
      },
    });

    const stockSubscription = this.productService.getProductStock(product.id).subscribe({
      next: (stockInfo) => {
        if (stockInfo) {
          // Update the editing product with detailed stock information
          const current = this.editing();
          if (current) {
            const updatedProduct = {
              ...current,
              currentStock: stockInfo.quantityAvailable,
              _stockDetails: {
                onHand: stockInfo.quantityOnHand,
                reserved: stockInfo.quantityReserved,
                available: stockInfo.quantityAvailable,
              }
            };
            this.editing.set(updatedProduct);
          }
        }
      },
      error: (err) => {
        console.error('Error loading product stock info:', err);
      },
    });

    // Cleanup subscription
    this.destroyRef.onDestroy(() => {
      detailsSubscription.unsubscribe();
      suppliersSubscription.unsubscribe();
      stockSubscription.unsubscribe();
    });
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
    this.detailsLoading.set(false);
  }

  // Refresh data method
  refreshData() {
    this.loadData();
  }
}