import { Component, computed, signal, inject, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../shared/header/header';
import { SideNav } from '../../shared/side-nav/side-nav';
import { SupplierListingComponent } from '../../suppliers/supplier-listing/supplier-listing';
import { SupplierEditor } from '../../suppliers/supplier-editor/supplier-editor';
import { Supplier } from '../../models/supplier.model';
import { Product } from '../../models/product.model';
import { DataService } from '../data.service'; // Import the new DataService

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
  private dataService = inject(DataService); // Use DataService instead of MockDataService
  private destroyRef = inject(DestroyRef);

  query = signal('');
  editorOpen = signal(false);
  editing = signal<Supplier | null>(null);
  
  // Filter states (can be extended later for supplier-specific filters)
  filterOpen = signal(false);
  
  // Loading state
  loading = signal(false);

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
    const suppliersSubscription = this.dataService.getSuppliers().subscribe({
      next: (suppliers) => {
        // Ensure all suppliers have selected property set to false
        const suppliersWithSelection = suppliers.map(s => ({ ...s, selected: false }));
        this.all.set(suppliersWithSelection);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading suppliers:', err);
        this.loading.set(false);
      },
    });

    // Load products to check relationships
    const productsSubscription = this.dataService.getProducts().subscribe({
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
    const newSupplier: Supplier = {
      id: '',
      name: '',
      email: '',
      phone: '',
      city: '',
      selected: false,
    };
    this.editing.set(newSupplier);
    this.editorOpen.set(true);
    console.log('Editor opened with:', newSupplier);
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
    
    this.all.update((list) => list.filter((s) => !s.selected));
    this.bumpSelection();
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
  handleSave(updated: Supplier) {
    console.log('handleSave called with:', updated);
    
    // Validate required fields
    if (!updated.name?.trim() || !updated.email?.trim() || !updated.phone?.trim() || !updated.city?.trim()) {
      console.error('Missing required fields');
      return;
    }

    // Ensure we have clean data
    const cleanUpdated: Supplier = {
      id: updated.id?.trim() || '',
      name: updated.name.trim(),
      email: updated.email.trim(),
      phone: updated.phone.trim(),
      city: updated.city.trim(),
      selected: updated.selected || false,
    };
    
    // Check if this is an update (existing supplier with ID) or new supplier
    const isUpdate = cleanUpdated.id && this.all().some((s) => s.id === cleanUpdated.id);
    console.log('Is update:', isUpdate, 'ID:', cleanUpdated.id);
    
    if (isUpdate) {
      // Update existing supplier
      console.log('Updating existing supplier');
      this.all.update((list) => {
        const newList = list.map((s) => 
          s.id === cleanUpdated.id 
            ? { ...cleanUpdated, selected: s.selected } 
            : s
        );
        console.log('New list after update:', newList);
        return newList;
      });
    } else {
      // Add new supplier - generate ID if empty
      const maxIdNumber = Math.max(
        ...this.all().map(s => {
          const match = s.id.match(/SUP-(\d+)/);
          return match ? parseInt(match[1]) : 0;
        }),
        0
      );
      const id = cleanUpdated.id || `SUP-${String(maxIdNumber + 1).padStart(3, '0')}`;
      const newSupplier: Supplier = { 
        ...cleanUpdated, 
        id, 
        selected: false 
      };
      console.log('Adding new supplier:', newSupplier);
      
      // Check for duplicate IDs
      if (this.all().some(s => s.id === id)) {
        console.warn('Duplicate ID detected, generating new one');
        newSupplier.id = `SUP-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
      }
      
      this.all.update((list) => {
        const newList = [newSupplier, ...list];
        console.log('New list after addition:', newList);
        return newList;
      });
    }
    
    this.closeEditor();
    console.log('Final suppliers list:', this.all());
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
    
    this.all.update((list) => list.filter((s) => s.id !== id));
    this.closeEditor();
    console.log('Supplier deleted, remaining suppliers:', this.all());
  }

  closeEditor() {
    console.log('Closing editor');
    this.editorOpen.set(false);
    this.editing.set(null);
  }

  // Refresh data method
  refreshData() {
    this.loadData();
  }
}