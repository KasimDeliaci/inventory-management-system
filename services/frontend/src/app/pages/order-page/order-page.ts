// order-page.ts
import { Component, computed, signal, inject, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../shared/header/header';
import { SideNav } from '../../shared/side-nav/side-nav';
import { OrderListingComponent } from '../../orders/order-listing/order-listing';
import { OrderEditorComponent } from '../../orders/order-editor/order-editor';
import { Order, OrderType, PurchaseOrderStatus, SalesOrderStatus } from '../../models/order.model';
import { Supplier } from '../../models/supplier.model';
import { Customer } from '../../models/customer.model';
import { Product } from '../../models/product.model';
import { DataService } from '../data.service';

// Extended order interface to include items
interface OrderWithItems extends Order {
  items?: Array<{
    productId: string;
    productName?: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
    discountPercentage?: number;
    discountAmount?: number;
    campaignId?: string;
  }>;
}

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
export class OrderPageComponent implements OnInit {
  private dataService = inject(DataService);
  private destroyRef = inject(DestroyRef);

  query = signal('');
  editorOpen = signal(false);
  editing = signal<OrderWithItems | null>(null);
  
  // Filter states
  filterOpen = signal(false);
  typeFilter = signal<OrderType | 'all'>('all');
  statusFilter = signal<PurchaseOrderStatus | SalesOrderStatus | 'all'>('all');

  // Loading states
  loading = signal(false);
  detailsLoading = signal(false);

  private selectionTick = signal(0);
  
  bumpSelection() {
    this.selectionTick.update((n) => n + 1);
  }

  // Initialize with signals
  private all = signal<Order[]>([]);
  suppliers = signal<Supplier[]>([]);
  customers = signal<Customer[]>([]);
  products = signal<Product[]>([]);

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

  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    this.loading.set(true);
    
    // Load orders from backend
    const ordersSubscription = this.dataService.getOrders().subscribe({
      next: (orders) => {
        this.all.set(orders);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading orders:', err);
        this.loading.set(false);
      },
    });

    // Load base data for dropdowns (suppliers, customers, products)
    const suppliersSubscription = this.dataService.getSuppliers().subscribe({
      next: (suppliers) => {
        this.suppliers.set(suppliers);
      },
      error: (err) => {
        console.error('Error loading suppliers:', err);
      },
    });

    const customersSubscription = this.dataService.getCustomers().subscribe({
      next: (customers) => {
        this.customers.set(customers);
      },
      error: (err) => {
        console.error('Error loading customers:', err);
      },
    });

    const productsSubscription = this.dataService.getProducts().subscribe({
      next: (products) => {
        this.products.set(products);
      },
      error: (err) => {
        console.error('Error loading products:', err);
      },
    });

    // Cleanup subscriptions when component is destroyed
    this.destroyRef.onDestroy(() => {
      ordersSubscription.unsubscribe();
      suppliersSubscription.unsubscribe();
      customersSubscription.unsubscribe();
      productsSubscription.unsubscribe();
    });
  }

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
      items: [],
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
      items: [],
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
    // Set loading state
    this.detailsLoading.set(true);
    this.editorOpen.set(true);
    
    // Fetch detailed order information including related data and items
    const detailsSubscription = this.dataService.getOrderDetails(order).subscribe({
      next: (orderDetails) => {
        if (orderDetails) {
          // Transform order items to our internal format
          const items = this.transformOrderItems(orderDetails.orderItems, orderDetails.products);
          
          // Set the editing order with detailed information
          const orderWithItems: OrderWithItems = {
            ...orderDetails.order,
            items,
          };
          
          this.editing.set(orderWithItems);
          
          // Update suppliers/customers based on order type
          if (order.type === 'sales' && orderDetails.customer) {
            // Make sure the customer is in our customers list
            const currentCustomers = this.customers();
            const customerExists = currentCustomers.some(c => c.id === orderDetails.customer!.id);
            if (!customerExists) {
              this.customers.update(customers => [...customers, orderDetails.customer!]);
            }
          } else if (order.type === 'purchase' && 'supplier' in orderDetails && orderDetails.supplier) {
            // Make sure the supplier is in our suppliers list
            const currentSuppliers = this.suppliers();
            const supplierExists = currentSuppliers.some(s => s.id === orderDetails.supplier!.id);
            if (!supplierExists) {
              this.suppliers.update(suppliers => [...suppliers, orderDetails.supplier!]);
            }
          }
          
          // Update products if we got order-specific products
          if (orderDetails.products && orderDetails.products.length > 0) {
            this.products.set(orderDetails.products);
          }

          // Update the order in our main list with the calculated totals
          this.all.update(list => 
            list.map(o => 
              o.id === order.id 
                ? { ...o, quantity: orderDetails.order.quantity, totalPrice: orderDetails.order.totalPrice }
                : o
            )
          );
        } else {
          // Fallback to the basic order data from the list
          console.warn('Could not load detailed order data, using basic info');
          this.editing.set({ ...order, items: [] });
        }
        this.detailsLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading order details:', err);
        // Fallback to basic order data
        this.editing.set({ ...order, items: [] });
        this.detailsLoading.set(false);
      },
    });

    // Cleanup subscription
    this.destroyRef.onDestroy(() => {
      detailsSubscription.unsubscribe();
    });
  }

  // Transform backend order items to our internal format
  private transformOrderItems(orderItems: any[], products: Product[]): OrderWithItems['items'] {
    if (!orderItems || orderItems.length === 0) {
      return [];
    }

    return orderItems.map(item => {
      // Find the product to get the name
      const productId = `ID-${item.productId.toString().padStart(3, '0')}`;
      const product = products.find(p => p.id === productId);

      return {
        productId,
        productName: product?.name || `Product ${item.productId}`,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        lineTotal: item.lineTotal,
        discountPercentage: item.discountPercentage || 0,
        discountAmount: item.discountAmount || 0,
        campaignId: item.campaignId ? `CAMP-${item.campaignId.toString().padStart(3, '0')}` : undefined,
      };
    });
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
      const id = updated.id?.trim() || `${prefix}-${String(existingIds + 1).padStart(5, '0')}`;
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
    this.detailsLoading.set(false);
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

  // Refresh data method
  refreshData() {
    this.loadData();
  }

  // Helper method to get order items summary for display
  getOrderItemsSummary(order: Order): string {
    // Since we now load totals upfront, we can use the calculated values
    const itemCount = order.quantity || 0;
    
    if (itemCount === 0) {
      return 'No items';
    }
    
    if (itemCount === 1) {
      return '1 item';
    }
    
    return `${itemCount} items`;
  }

  // Helper method to check if order has detailed items loaded
  hasOrderItems(order: Order): boolean {
    // Orders now have calculated totals from page load
    return (order.quantity || 0) > 0;
  }
}