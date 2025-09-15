import { Component, computed, signal, inject, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../shared/header/header';
import { SideNav } from '../../shared/side-nav/side-nav';
import { SupplierListingComponent } from '../../suppliers/supplier-listing/supplier-listing';
import { SupplierEditor } from '../../suppliers/supplier-editor/supplier-editor';
import { Supplier } from '../../models/supplier.model';
import { Product } from '../../models/product.model';
import { SupplierService } from '../supplier.service';
import { ProductService } from '../product.service';

@Component({
  selector: 'app-supplier-page',
  standalone: true,
  imports: [
    CommonModule,
    Header,
    SideNav,
    SupplierListingComponent,
    SupplierEditor,
  ],
  templateUrl: './supplier-page.html',
  styleUrls: ['./supplier-page.scss'],
})
export class SupplierPageComponent implements OnInit {
  private supplierService = inject(SupplierService);
  private productService = inject(ProductService);
  private destroyRef = inject(DestroyRef);

  query = signal('');
  editorOpen = signal(false);
  editing = signal<Supplier | null>(null);
  
  // Filter states (can be extended later for supplier-specific filters)
  filterOpen = signal(false);
  
  // Loading states
  loading = signal(false);
  deleting = signal(false);
  saving = signal(false); // Add saving state

  private selectionTick = signal(0);
  
  bumpSelection() {
    this.selectionTick.update((n) => n + 1);
  }

  // Initialize with signals
  private all = signal<Supplier[]>([]);
  private allProducts = signal<Product[]>([]);

  readonly suppliers = computed(() => {
    let filtered = this.all();
    
    // Apply search query
    const q = this.query().trim().toLowerCase();
    if (q) {
      filtered = filtered.filter((s) =>
        [
          s.id,
          s.name,
          s.email,
          s.phone,
          s.city,
        ].some((v) => typeof v === 'string' && v.toLowerCase().includes(q))
      );
    }
    
    return filtered;
  });

  /** Depends on selectionTick so it updates when checkboxes toggle */
  selectedCount = computed(() => {
    this.selectionTick();
    return this.all().filter((s) => s.selected).length;
  });

  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    this.loading.set(true);
    
    // Load suppliers from backend
    const suppliersSubscription = this.supplierService.getSuppliers().subscribe({
      next: (suppliers) => {
        // Ensure all suppliers have selected property set to false
        const suppliersWithSelection = suppliers.map(s => ({ ...s, selected: false }));
        this.all.set(suppliersWithSelection);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading suppliers:', err);
        this.loading.set(false);
        this.showErrorMessage('Failed to load suppliers. Please try again.');
      },
    });

    // Load products to check relationships
    const productsSubscription = this.productService.getProducts().subscribe({
      next: (products) => {
        this.allProducts.set(products);
      },
      error: (err) => {
        console.error('Error loading products:', err);
      },
    });

    // Cleanup subscriptions
    this.destroyRef.onDestroy(() => {
      suppliersSubscription.unsubscribe();
      productsSubscription.unsubscribe();
    });
  }

  // Get products associated with a supplier
  getSupplierProducts(supplierId: string): Product[] {
    return this.allProducts().filter(p => 
      p.preferredSupplierId === supplierId || 
      p.activeSupplierIds.includes(supplierId)
    );
  }

  // Header event handlers
  onAddSupplier() {
    console.log('Adding new supplier');
    // Set editing to null for new supplier
    this.editing.set(null);
    this.editorOpen.set(true);
    console.log('Editor opened for new supplier');
  }

  onOpenFilters() {
    this.filterOpen.set(!this.filterOpen());
  }

  onClear() {
    this.query.set('');
  }

  deleteSelected() {
    const count = this.selectedCount();
    if (count === 0) return;
    
    const message = `Delete ${count} selected supplier${count > 1 ? 's' : ''}?`;
    if (!confirm(message)) return;
    
    // Get IDs of suppliers being deleted
    const deletingIds = this.all().filter(s => s.selected).map(s => s.id);
    
    // Check if any products are associated with these suppliers
    const affectedProducts = this.allProducts().filter(p => 
      deletingIds.some(id => 
        p.preferredSupplierId === id || 
        p.activeSupplierIds.includes(id)
      )
    );
    
    if (affectedProducts.length > 0) {
      const productNames = affectedProducts.slice(0, 3).map(p => p.name).join(', ');
      const moreText = affectedProducts.length > 3 ? ` and ${affectedProducts.length - 3} more` : '';
      const warning = `These suppliers are associated with products: ${productNames}${moreText}. Continue with deletion?`;
      if (!confirm(warning)) return;
    }
    
    this.deleting.set(true);
    
    // Perform HTTP deletion
    const deleteSubscription = this.supplierService.deleteMultipleSuppliers(deletingIds).subscribe({
      next: () => {
        console.log('Successfully deleted suppliers from backend');
        // Remove from local state only after successful backend deletion
        this.all.update((list) => list.filter((s) => !s.selected));
        this.bumpSelection();
        this.deleting.set(false);
        this.showSuccessMessage(`Successfully deleted ${deletingIds.length} supplier${deletingIds.length > 1 ? 's' : ''}`);
      },
      error: (error) => {
        console.error('Error deleting suppliers:', error);
        this.deleting.set(false);
        this.showErrorMessage('Failed to delete suppliers. Please try again.');
      }
    });

    this.destroyRef.onDestroy(() => deleteSubscription.unsubscribe());
  }

  openEditorFor(supplier: Supplier) {
    console.log('Opening editor for supplier:', supplier);
    // Create a deep copy to avoid modifying the original
    this.editing.set({ 
      ...supplier,
      selected: supplier.selected || false 
    });
    this.editorOpen.set(true);
  }

  // Editor event handlers
  handleSave(supplierData: Supplier) {
    console.log('handleSave called with:', supplierData);
    
    // Validate required fields
    if (!supplierData.name?.trim() || !supplierData.email?.trim() || 
        !supplierData.phone?.trim() || !supplierData.city?.trim()) {
      console.error('Missing required fields');
      this.showErrorMessage('Please fill in all required fields');
      return;
    }

    // Ensure we have clean data
    const cleanSupplier: Supplier = {
      id: supplierData.id?.trim() || '',
      name: supplierData.name.trim(),
      email: supplierData.email.trim(),
      phone: supplierData.phone.trim(),
      city: supplierData.city.trim(),
      selected: supplierData.selected || false,
    };
    
    this.saving.set(true);

    // Determine if this is an update or create operation
    const isUpdate = cleanSupplier.id && cleanSupplier.id !== '' && 
                     this.all().some((s) => s.id === cleanSupplier.id);
    
    console.log('Is update:', isUpdate, 'ID:', cleanSupplier.id);
    
    const saveOperation = isUpdate 
      ? this.supplierService.updateSupplier(cleanSupplier)
      : this.supplierService.createSupplier(cleanSupplier);

    const saveSubscription = saveOperation.subscribe({
      next: (savedSupplier) => {
        console.log('Successfully saved supplier:', savedSupplier);
        
        if (isUpdate) {
          // Update existing supplier in local state
          this.all.update((list) => 
            list.map((s) => 
              s.id === cleanSupplier.id 
                ? { ...savedSupplier, selected: s.selected } 
                : s
            )
          );
          this.showSuccessMessage('Supplier updated successfully');
        } else {
          // Add new supplier to local state
          const newSupplier = { ...savedSupplier, selected: false };
          this.all.update((list) => [newSupplier, ...list]);
          this.showSuccessMessage('Supplier created successfully');
        }
        
        this.saving.set(false);
        this.closeEditor();
      },
      error: (error) => {
        console.error('Error saving supplier:', error);
        this.saving.set(false);
        const action = isUpdate ? 'update' : 'create';
        this.showErrorMessage(`Failed to ${action} supplier. Please try again.`);
      }
    });

    this.destroyRef.onDestroy(() => saveSubscription.unsubscribe());
  }

  handleDelete(id: string) {
    console.log('handleDelete called with id:', id);
    
    if (!id) {
      console.error('No ID provided for deletion');
      return;
    }

    // Check if supplier is associated with any products
    const associatedProducts = this.getSupplierProducts(id);
    
    if (associatedProducts.length > 0) {
      const productNames = associatedProducts.slice(0, 3).map(p => p.name).join(', ');
      const moreText = associatedProducts.length > 3 ? ` and ${associatedProducts.length - 3} more` : '';
      const warning = `This supplier is associated with products: ${productNames}${moreText}. Are you sure you want to delete it?`;
      if (!confirm(warning)) return;
    } else {
      if (!confirm('Are you sure you want to delete this supplier?')) return;
    }
    
    this.deleting.set(true);
    
    // Perform HTTP deletion
    const deleteSubscription = this.supplierService.deleteSupplier(id).subscribe({
      next: () => {
        console.log('Successfully deleted supplier from backend');
        // Remove from local state only after successful backend deletion
        this.all.update((list) => list.filter((s) => s.id !== id));
        this.closeEditor();
        this.deleting.set(false);
        this.showSuccessMessage('Supplier deleted successfully');
        console.log('Supplier deleted, remaining suppliers:', this.all());
      },
      error: (error) => {
        console.error('Error deleting supplier:', error);
        this.deleting.set(false);
        this.showErrorMessage('Failed to delete supplier. Please try again.');
      }
    });

    this.destroyRef.onDestroy(() => deleteSubscription.unsubscribe());
  }

  closeEditor() {
    console.log('Closing editor');
    this.editorOpen.set(false);
    this.editing.set(null);
    this.saving.set(false); // Reset saving state when closing
  }

  // Refresh data method
  refreshData() {
    this.loadData();
  }

  // Helper methods for user feedback
  private showSuccessMessage(message: string) {
    // Replace with your preferred notification system
    console.log('Success:', message);
    // You might want to use a toast library or other notification system
    // For now, we'll use a simple alert - replace with better UX
    // alert(message);
  }

  private showErrorMessage(message: string) {
    // Replace with your preferred notification system
    console.error('Error:', message);
    alert(message); // Replace with better error handling/display
  }
}