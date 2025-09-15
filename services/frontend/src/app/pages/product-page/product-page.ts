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
  
  // Loading states
  loading = signal(false);
  detailsLoading = signal(false);
  deleting = signal(false);
  bulkDeleting = signal(false);
  saving = signal(false); // New saving state

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
    // Load suppliers when adding a new product
    const suppliersSubscription = this.supplierService.getSuppliers().subscribe({
      next: (suppliers) => {
        this.suppliers.set(suppliers);
      },
      error: (err) => {
        console.error('Error loading suppliers:', err);
      },
    });

    this.destroyRef.onDestroy(() => {
      suppliersSubscription.unsubscribe();
    });

    this.editing.set({
      id: '',
      name: '',
      category: '',
      unit: '',
      description: null,
      price: null,
      safetyStock: null,
      reorderPoint: null,
      currentStock: 0,
      preferredSupplierId: null,
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

  // Updated bulk delete method with backend API call
  deleteSelected() {
    const selectedProducts = this.all().filter(p => p.selected);
    const count = selectedProducts.length;
    
    if (count === 0) return;
    
    const message = `Delete ${count} selected product${count > 1 ? 's' : ''}?`;
    if (!confirm(message)) return;
    
    this.bulkDeleting.set(true);
    const productIds = selectedProducts.map(p => p.id);
    
    const deleteSubscription = this.productService.deleteMultipleProducts(productIds).subscribe({
      next: (result) => {
        if (result.success.length > 0) {
          // Remove successfully deleted products from local state
          this.all.update((list) => 
            list.filter((p) => !result.success.includes(p.id))
          );
          this.bumpSelection();
          
          if (result.success.length === count) {
            console.log(`Successfully deleted ${result.success.length} product(s)`);
          } else {
            console.log(`Deleted ${result.success.length} of ${count} products`);
            if (result.failed.length > 0) {
              alert(`Failed to delete ${result.failed.length} product(s). Please try again.`);
            }
          }
        } else {
          alert('Failed to delete selected products. Please try again.');
        }
        this.bulkDeleting.set(false);
      },
      error: (err) => {
        console.error('Error during bulk delete:', err);
        alert('An error occurred while deleting products. Please try again.');
        this.bulkDeleting.set(false);
      },
    });

    this.destroyRef.onDestroy(() => {
      deleteSubscription.unsubscribe();
    });
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

  // Updated save handler with backend API calls
  handleSave(updated: Product) {
    // Validate required fields
    if (!updated.name?.trim()) {
      alert('Product name is required.');
      return;
    }
    if (!updated.category?.trim()) {
      alert('Category is required.');
      return;
    }
    if (!updated.unit?.trim()) {
      alert('Unit of measure is required.');
      return;
    }

    this.saving.set(true);
    
    const isNewProduct = !updated.id || !this.all().some((p) => p.id === updated.id);
    
    if (isNewProduct) {
      // Create new product via API
      const createSubscription = this.productService.createProduct(updated).subscribe({
        next: (createdProduct) => {
          if (createdProduct) {
            // Add the new product to the beginning of the list
            this.all.update((list) => [createdProduct, ...list]);
            console.log('Product created successfully:', createdProduct);
            this.closeEditor();
          } else {
            alert('Failed to create product. Please try again.');
          }
          this.saving.set(false);
        },
        error: (err) => {
          console.error('Error creating product:', err);
          alert('An error occurred while creating the product. Please try again.');
          this.saving.set(false);
        },
      });

      this.destroyRef.onDestroy(() => {
        createSubscription.unsubscribe();
      });
    } else {
      // Update existing product via API
      const updateSubscription = this.productService.updateProduct(updated).subscribe({
        next: (updatedProduct) => {
          if (updatedProduct) {
            // Update the product in the local list
            this.all.update((list) => 
              list.map((p) => (p.id === updated.id ? { ...p, ...updatedProduct } : p))
            );
            console.log('Product updated successfully:', updatedProduct);
            this.closeEditor();
          } else {
            alert('Failed to update product. Please try again.');
          }
          this.saving.set(false);
        },
        error: (err) => {
          console.error('Error updating product:', err);
          alert('An error occurred while updating the product. Please try again.');
          this.saving.set(false);
        },
      });

      this.destroyRef.onDestroy(() => {
        updateSubscription.unsubscribe();
      });
    }
  }

  // Updated delete method with backend API call
  handleDelete(id: string) {
    if (!confirm('Are you sure you want to delete this product?')) {
      return;
    }
    
    this.deleting.set(true);
    
    const deleteSubscription = this.productService.deleteProduct(id).subscribe({
      next: (success) => {
        if (success) {
          // Remove from local state only if backend deletion was successful
          this.all.update((list) => list.filter((p) => p.id !== id));
          this.closeEditor();
          console.log(`Product ${id} deleted successfully`);
        } else {
          alert('Failed to delete product. Please try again.');
        }
        this.deleting.set(false);
      },
      error: (err) => {
        console.error('Error deleting product:', err);
        alert('An error occurred while deleting the product. Please try again.');
        this.deleting.set(false);
      },
    });

    this.destroyRef.onDestroy(() => {
      deleteSubscription.unsubscribe();
    });
  }

  closeEditor() {
    this.editorOpen.set(false);
    this.editing.set(null);
    this.detailsLoading.set(false);
    this.deleting.set(false);
    this.saving.set(false);
  }

  // Refresh data method
  refreshData() {
    this.loadData();
  }

  // Getter for template to check if any operation is in progress
  get isOperationInProgress(): boolean {
    return this.deleting() || this.bulkDeleting() || this.saving();
  }
}