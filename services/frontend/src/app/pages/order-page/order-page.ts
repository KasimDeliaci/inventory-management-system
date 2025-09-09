// order-page.ts
import { Component, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../shared/header/header';
import { SideNav } from '../../shared/side-nav/side-nav';
import { OrderListingComponent } from '../../orders/order-listing/order-listing';
import { OrderEditorComponent } from '../../orders/order-editor/order-editor';
import { Order, OrderType, PurchaseOrderStatus, SalesOrderStatus } from '../../models/order.model';
import { Supplier } from '../../models/supplier.model';
import { Customer } from '../../models/customer.model';
import { Product } from '../../models/product.model';
import { MockDataService } from '../mock-data.service';

@Component({
  selector: 'app-order-page',
  standalone: true,
  imports: [
    CommonModule,
    Header,
    SideNav,
    OrderListingComponent,
    OrderEditorComponent,
  ],
  templateUrl: './order-page.html',
  styleUrls: ['./order-page.scss'],
})
export class OrderPageComponent {
  private mockDataService = inject(MockDataService);

  query = signal('');
  editorOpen = signal(false);
  editing = signal<Order | null>(null);
  
  // Filter states
  filterOpen = signal(false);
  typeFilter = signal<OrderType | 'all'>('all');
  statusFilter = signal<PurchaseOrderStatus | SalesOrderStatus | 'all'>('all');

  private selectionTick = signal(0);
  
  bumpSelection() {
    this.selectionTick.update((n) => n + 1);
  }

  // Initialize with mock data
  private all = signal<Order[]>(this.mockDataService.getOrders());
  suppliers = signal<Supplier[]>(this.mockDataService.getSuppliers());
  customers = signal<Customer[]>(this.mockDataService.getCustomers());
  products = signal<Product[]>(this.mockDataService.getProducts());

  readonly orders = computed(() => {
    let filtered = this.all();
    
    // Apply type filter
    const typeFilter = this.typeFilter();
    if (typeFilter !== 'all') {
      filtered = filtered.filter(o => o.type === typeFilter);
    }
    
    // Apply status filter
    const statusFilter = this.statusFilter();
    if (statusFilter !== 'all') {
      filtered = filtered.filter(o => o.status === statusFilter);
    }
    
    // Apply search query
    const q = this.query().trim().toLowerCase();
    if (q) {
      filtered = filtered.filter((o) =>
        [
          o.id,
          o.supplierId,
          o.customerId,
          o.productId,
          o.status,
          o.type,
        ].some((v) => typeof v === 'string' && v.toLowerCase().includes(q))
      );
    }
    
    return filtered;
  });

  // Depends on selectionTick so it updates when checkboxes toggle 
  selectedCount = computed(() => {
    this.selectionTick();
    return this.all().filter((o) => o.selected).length;
  });

  // Status change handler
  onStatusChange(event: { order: Order, newStatus: PurchaseOrderStatus | SalesOrderStatus }) {
    this.all.update((list) => 
      list.map((o) => 
        o.id === event.order.id 
          ? { ...o, status: event.newStatus }
          : o
      )
    );
  }

  // Header event handlers
  onAddPurchaseOrder() {
    this.editing.set({
      id: '',
      type: 'purchase',
      supplierId: '',
      orderDate: new Date().toISOString().split('T')[0],
      deliveryDate: '',
      quantity: 0,
      totalPrice: 0,
      status: 'placed',
      selected: false,
    });
    this.editorOpen.set(true);
  }

  onAddSalesOrder() {
    this.editing.set({
      id: '',
      type: 'sales',
      customerId: '',
      orderDate: new Date().toISOString().split('T')[0],
      deliveryDate: '',
      quantity: 0,
      totalPrice: 0,
      status: 'pending',
      selected: false,
    });
    this.editorOpen.set(true);
  }

  onOpenFilters() {
    this.filterOpen.set(!this.filterOpen());
  }

  onTypeFilterSelect(type: OrderType | 'all') {
    this.typeFilter.set(type);
    // Reset status filter when changing type
    this.statusFilter.set('all');
    this.filterOpen.set(false);
  }

  onStatusFilterSelect(status: PurchaseOrderStatus | SalesOrderStatus | 'all') {
    this.statusFilter.set(status);
    this.filterOpen.set(false);
  }

  onCustomAction(action: string) {
    switch (action) {
      case 'add-purchase':
        this.onAddPurchaseOrder();
        break;
      case 'add-sales':
        this.onAddSalesOrder();
        break;
    }
  }

  getCustomActionButtons() {
    return [
      { text: '+ Purchase Order', action: 'add-purchase' },
      { text: '+ Sales Order', action: 'add-sales' }
    ];
  }

  onClear() {
    this.query.set('');
  }

  deleteSelected() {
    const count = this.selectedCount();
    if (count === 0) return;
    
    const message = `Delete ${count} selected order${count > 1 ? 's' : ''}?`;
    if (!confirm(message)) return;
    
    this.all.update((list) => list.filter((o) => !o.selected));
    this.bumpSelection();
  }

  openEditorFor(order: Order) {
    this.editing.set({ ...order });
    this.editorOpen.set(true);
  }

  // Editor event handlers
  handleSave(updated: Order) {
    if (updated.id && this.all().some((o) => o.id === updated.id)) {
      // Update existing order
      this.all.update((list) => 
        list.map((o) => (o.id === updated.id ? { ...o, ...updated } : o))
      );
    } else {
      // Add new order
      const prefix = updated.type === 'purchase' ? 'PO' : 'SO';
      const existingIds = this.all().filter(o => o.type === updated.type).length;
      const id = updated.id?.trim() || `${prefix}-${String(existingIds + 1).padStart(3, '0')}`;
      this.all.update((list) => [{ ...updated, id, selected: false }, ...list]);
    }
    this.closeEditor();
  }

  handleDelete(id: string) {
    if (confirm('Are you sure you want to delete this order?')) {
      this.all.update((list) => list.filter((o) => o.id !== id));
      this.closeEditor();
    }
  }

  closeEditor() {
    this.editorOpen.set(false);
    this.editing.set(null);
  }

  // Get display name for supplier/customer
  getSupplierName(id: string | undefined): string {
    if (!id) return '';
    const supplier = this.suppliers().find(s => s.id === id);
    return supplier ? supplier.name : id;
  }

  getCustomerName(id: string | undefined): string {
    if (!id) return '';
    const customer = this.customers().find(c => c.id === id);
    return customer ? customer.name : id;
  }

  getProductName(id: string | undefined): string {
    if (!id) return '';
    const product = this.products().find(p => p.id === id);
    return product ? product.name : id;
  }
}