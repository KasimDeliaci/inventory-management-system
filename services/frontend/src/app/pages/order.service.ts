import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, catchError, of, switchMap, from, mergeMap, toArray } from 'rxjs';
import { Product } from '../models/product.model';
import { Supplier } from '../models/supplier.model';
import { Customer } from '../models/customer.model';
import { Order, OrderType, PurchaseOrderStatus, SalesOrderStatus } from '../models/order.model';
import {
  BackendSalesOrder,
  BackendPurchaseOrder,
  BackendSalesOrderResponse,
  BackendPurchaseOrderResponse,
} from '../models/backend.model';
import { SupplierService } from './supplier.service';
import { CustomerService } from './customer.service';
import { ProductService } from './product.service';

// Add new interfaces for order items
interface BackendSalesOrderItem {
  salesOrderItemId: number;
  salesOrderId: number;
  productId: number;
  quantity: number;
  unitPrice: number;
  discountPercentage: number;
  campaignId: number | null;
  discountAmount: number;
  lineTotal: number;
  createdAt: string;
}

interface BackendPurchaseOrderItem {
  purchaseOrderItemId: number;
  purchaseOrderId: number;
  productId: number;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  createdAt: string;
}

interface BackendOrderItemResponse<T> {
  content: T[];
  page: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private httpClient = inject(HttpClient);
  private supplierService = inject(SupplierService);
  private customerService = inject(CustomerService);
  private productService = inject(ProductService);
  private readonly baseUrl = 'http://localhost:8000/api/v1';

  // UPDATED ORDER METHODS - NOW FETCHES ORDER ITEMS UPFRONT
  getOrders(): Observable<Order[]> {
    return forkJoin({
      salesOrders: this.httpClient
        .get<BackendSalesOrderResponse>(`${this.baseUrl}/sales-orders`)
        .pipe(
          catchError((error) => {
            console.error('Error fetching sales orders:', error);
            return of({ content: [], page: { page: 0, size: 0, totalElements: 0, totalPages: 0 } });
          })
        ),
      purchaseOrders: this.httpClient
        .get<BackendPurchaseOrderResponse>(`${this.baseUrl}/purchase-orders`)
        .pipe(
          catchError((error) => {
            console.error('Error fetching purchase orders:', error);
            return of({ content: [], page: { page: 0, size: 0, totalElements: 0, totalPages: 0 } });
          })
        ),
    }).pipe(
      switchMap(({ salesOrders, purchaseOrders }) => {
        // Transform orders to our format first
        const salesOrderList = this.transformBackendSalesOrders(salesOrders.content);
        const purchaseOrderList = this.transformBackendPurchaseOrders(purchaseOrders.content);
        
        // Create requests for all order items
        const itemRequests: Observable<any>[] = [];
        
        // Add sales order items requests
        salesOrderList.forEach(order => {
          const numericId = this.extractNumericId(order.id);
          if (numericId) {
            itemRequests.push(
              this.httpClient.get<BackendOrderItemResponse<BackendSalesOrderItem>>(
                `${this.baseUrl}/sales-orders/${numericId}/items`
              ).pipe(
                map(response => ({ orderId: order.id, items: response.content, type: 'sales' })),
                catchError(() => of({ orderId: order.id, items: [], type: 'sales' }))
              )
            );
          }
        });
        
        // Add purchase order items requests
        purchaseOrderList.forEach(order => {
          const numericId = this.extractNumericId(order.id);
          if (numericId) {
            itemRequests.push(
              this.httpClient.get<BackendOrderItemResponse<BackendPurchaseOrderItem>>(
                `${this.baseUrl}/purchase-orders/${numericId}/items`
              ).pipe(
                map(response => ({ orderId: order.id, items: response.content, type: 'purchase' })),
                catchError(() => of({ orderId: order.id, items: [], type: 'purchase' }))
              )
            );
          }
        });
        
        // If no items to fetch, return orders as-is
        if (itemRequests.length === 0) {
          return of([...salesOrderList, ...purchaseOrderList]);
        }
        
        // Execute all item requests with concurrency limit
        return from(itemRequests).pipe(
          mergeMap(request => request, 10), // Process max 10 concurrent requests
          toArray(), // Collect all results
          map((itemResults: any[]) => {
            // Create a map of order items by orderId
            const itemsMap = new Map();
            itemResults.forEach(result => {
              itemsMap.set(result.orderId, result.items);
            });
            
            // Update orders with calculated totals
            const updatedSalesOrders = salesOrderList.map(order => {
              const items = itemsMap.get(order.id) || [];
              const totalQuantity = items.reduce((sum: number, item: BackendSalesOrderItem) => sum + item.quantity, 0);
              const totalPrice = items.reduce((sum: number, item: BackendSalesOrderItem) => sum + item.lineTotal, 0);
              
              return {
                ...order,
                quantity: totalQuantity,
                totalPrice: totalPrice
              };
            });
            
            const updatedPurchaseOrders = purchaseOrderList.map(order => {
              const items = itemsMap.get(order.id) || [];
              const totalQuantity = items.reduce((sum: number, item: BackendPurchaseOrderItem) => sum + item.quantity, 0);
              const totalPrice = items.reduce((sum: number, item: BackendPurchaseOrderItem) => sum + item.lineTotal, 0);
              
              return {
                ...order,
                quantity: totalQuantity,
                totalPrice: totalPrice
              };
            });
            
            return [...updatedSalesOrders, ...updatedPurchaseOrders];
          })
        );
      }),
      catchError((error) => {
        console.error('Error fetching orders with items:', error);
        return of(this.getFallbackOrders());
      })
    );
  }

  // NEW: Get order items for sales orders
  getSalesOrderItems(salesOrderId: string): Observable<BackendSalesOrderItem[]> {
    const numericId = this.extractNumericId(salesOrderId);
    if (!numericId) {
      console.error('Invalid sales order ID format:', salesOrderId);
      return of([]);
    }

    return this.httpClient
      .get<BackendOrderItemResponse<BackendSalesOrderItem>>(`${this.baseUrl}/sales-orders/${numericId}/items`)
      .pipe(
        map((response) => response.content),
        catchError((error) => {
          console.error('Error fetching sales order items:', error);
          return of([]);
        })
      );
  }

  // NEW: Get order items for purchase orders (assuming similar API structure)
  getPurchaseOrderItems(purchaseOrderId: string): Observable<BackendPurchaseOrderItem[]> {
    const numericId = this.extractNumericId(purchaseOrderId);
    if (!numericId) {
      console.error('Invalid purchase order ID format:', purchaseOrderId);
      return of([]);
    }

    return this.httpClient
      .get<BackendOrderItemResponse<BackendPurchaseOrderItem>>(`${this.baseUrl}/purchase-orders/${numericId}/items`)
      .pipe(
        map((response) => response.content),
        catchError((error) => {
          console.error('Error fetching purchase order items:', error);
          return of([]);
        })
      );
  }

  getOrderDetails(order: Order): Observable<{
    order: Order;
    supplier?: Supplier;
    customer?: Customer;
    products: Product[];
    orderItems: any[];
  } | null> {
    if (order.type === 'sales') {
      return this.getSalesOrderDetails(order);
    } else {
      return this.getPurchaseOrderDetails(order);
    }
  }

  private getSalesOrderDetails(order: Order): Observable<{
    order: Order;
    customer?: Customer;
    products: Product[];
    orderItems: BackendSalesOrderItem[];
  } | null> {
    const requests: { [key: string]: Observable<any> } = {
      products: this.productService.getProducts(),
      orderItems: this.getSalesOrderItems(order.id),
    };

    if (order.customerId) {
      const numericCustomerId = this.extractNumericId(order.customerId);
      if (numericCustomerId) {
        requests['customers'] = this.customerService.getCustomers();
      }
    }

    return forkJoin(requests).pipe(
      map((results) => {
        const customer = order.customerId && results['customers'] 
          ? results['customers'].find((c: Customer) => c.id === order.customerId)
          : undefined;

        const orderItems: BackendSalesOrderItem[] = results['orderItems'] || [];
        
        // Calculate totals from order items
        const totalQuantity = orderItems.reduce((sum, item) => sum + item.quantity, 0);
        const totalPrice = orderItems.reduce((sum, item) => sum + item.lineTotal, 0);

        // Update order with calculated values
        const updatedOrder: Order = {
          ...order,
          quantity: totalQuantity,
          totalPrice: totalPrice,
        };

        return {
          order: updatedOrder,
          customer,
          products: results['products'] || [],
          orderItems,
        };
      }),
      catchError((error) => {
        console.error('Error fetching sales order details:', error);
        return of(null);
      })
    );
  }

  private getPurchaseOrderDetails(order: Order): Observable<{
    order: Order;
    supplier?: Supplier;
    products: Product[];
    orderItems: BackendPurchaseOrderItem[];
  } | null> {
    const requests: { [key: string]: Observable<any> } = {
      products: this.productService.getProducts(),
      orderItems: this.getPurchaseOrderItems(order.id),
    };

    if (order.supplierId) {
      const numericSupplierId = this.extractNumericId(order.supplierId);
      if (numericSupplierId) {
        requests['suppliers'] = this.supplierService.getSuppliers();
      }
    }

    return forkJoin(requests).pipe(
      map((results) => {
        const supplier = order.supplierId && results['suppliers']
          ? results['suppliers'].find((s: Supplier) => s.id === order.supplierId)
          : undefined;

        const orderItems: BackendPurchaseOrderItem[] = results['orderItems'] || [];
        
        // Calculate totals from order items
        const totalQuantity = orderItems.reduce((sum, item) => sum + item.quantity, 0);
        const totalPrice = orderItems.reduce((sum, item) => sum + item.lineTotal, 0);

        // Update order with calculated values
        const updatedOrder: Order = {
          ...order,
          quantity: totalQuantity,
          totalPrice: totalPrice,
        };

        return {
          order: updatedOrder,
          supplier,
          products: results['products'] || [],
          orderItems,
        };
      }),
      catchError((error) => {
        console.error('Error fetching purchase order details:', error);
        return of(null);
      })
    );
  }

  private transformBackendSalesOrders(backendOrders: BackendSalesOrder[]): Order[] {
    return backendOrders.map((backendOrder) => ({
      id: `SO-${backendOrder.salesOrderId.toString().padStart(5, '0')}`,
      type: 'sales' as OrderType,
      customerId: `CUST-${backendOrder.customerId.toString().padStart(3, '0')}`,
      orderDate: backendOrder.orderDate,
      deliveryDate: backendOrder.deliveryDate,
      quantity: 0, // Will be populated from order items during enrichment
      totalPrice: 0, // Will be populated from order items during enrichment
      status: this.mapBackendSalesOrderStatus(backendOrder.status),
      selected: false,
      notes: backendOrder.deliveredAt ? `Delivered at: ${backendOrder.deliveredAt}` : undefined,
    }));
  }

  private transformBackendPurchaseOrders(backendOrders: BackendPurchaseOrder[]): Order[] {
    return backendOrders.map((backendOrder) => ({
      id: `PO-${backendOrder.purchaseOrderId.toString().padStart(5, '0')}`,
      type: 'purchase' as OrderType,
      supplierId: `SUP-${backendOrder.supplierId.toString().padStart(3, '0')}`,
      orderDate: backendOrder.orderDate,
      deliveryDate: backendOrder.expectedDelivery,
      quantity: 0, // Will be populated from order items during enrichment
      totalPrice: 0, // Will be populated from order items during enrichment
      status: this.mapBackendPurchaseOrderStatus(backendOrder.status),
      selected: false,
      notes: backendOrder.actualDelivery ? `Delivered at: ${backendOrder.actualDelivery}` : undefined,
    }));
  }

  private mapBackendSalesOrderStatus(backendStatus: 'PENDING' | 'ALLOCATED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELED'): SalesOrderStatus {
    switch (backendStatus) {
      case 'PENDING':
        return 'pending';
      case 'ALLOCATED':
        return 'allocated';
      case 'IN_TRANSIT':
        return 'in_transit';
      case 'DELIVERED':
        return 'delivered';
      case 'CANCELED':
        return 'canceled';
      default:
        return 'pending';
    }
  }

  private mapBackendPurchaseOrderStatus(backendStatus: 'PLACED' | 'IN_TRANSIT' | 'RECEIVED' | 'CANCELED'): PurchaseOrderStatus {
    switch (backendStatus) {
      case 'PLACED':
        return 'placed';
      case 'IN_TRANSIT':
        return 'in_transit';
      case 'RECEIVED':
        return 'received';
      case 'CANCELED':
        return 'canceled';
      default:
        return 'placed';
    }
  }

  private extractNumericId(id: string): string | null {
    // Handle purchase order IDs
    if (id.startsWith('PO-')) {
      const numPart = id.substring(3);
      const numericValue = parseInt(numPart, 10);
      return numericValue.toString();
    }
    // Handle sales order IDs
    if (id.startsWith('SO-')) {
      const numPart = id.substring(3);
      const numericValue = parseInt(numPart, 10);
      return numericValue.toString();
    }
    // Handle customer IDs
    if (id.startsWith('CUST-')) {
      const numPart = id.substring(5);
      const numericValue = parseInt(numPart, 10);
      return numericValue.toString();
    }
    // Handle supplier IDs
    if (id.startsWith('SUP-')) {
      const numPart = id.substring(4);
      const numericValue = parseInt(numPart, 10);
      return numericValue.toString();
    }
    return id.match(/^\d+$/) ? id : null;
  }

  private getFallbackOrders(): Order[] {
    return [
      {
        id: 'SO-00001',
        type: 'sales',
        customerId: 'CUST-001',
        orderDate: '2024-09-10',
        deliveryDate: '2024-09-15',
        quantity: 50,
        totalPrice: 1299.50,
        status: 'allocated',
        selected: false,
        notes: 'Rush order for new store opening',
      },
      {
        id: 'PO-00001',
        type: 'purchase',
        supplierId: 'SUP-001',
        orderDate: '2024-09-08',
        deliveryDate: '2024-09-20',
        quantity: 100,
        totalPrice: 2599.00,
        status: 'placed',
        selected: false,
        notes: 'Monthly coffee bean restock',
      },
      {
        id: 'SO-00002',
        type: 'sales',
        customerId: 'CUST-002',
        orderDate: '2024-09-12',
        deliveryDate: '2024-09-14',
        quantity: 2,
        totalPrice: 25.98,
        status: 'in_transit',
        selected: false,
      },
      {
        id: 'PO-00002',
        type: 'purchase',
        supplierId: 'SUP-003',
        orderDate: '2024-09-11',
        deliveryDate: '2024-09-18',
        quantity: 30,
        totalPrice: 555.00,
        status: 'in_transit',
        selected: false,
        notes: 'Organic tea variety pack',
      },
      {
        id: 'SO-00003',
        type: 'sales',
        customerId: 'CUST-003',
        orderDate: '2024-09-05',
        deliveryDate: '2024-09-08',
        quantity: 25,
        totalPrice: 324.75,
        status: 'delivered',
        selected: false,
        notes: 'Delivered at: 2024-09-08T14:30:00Z',
      },
      {
        id: 'PO-00003',
        type: 'purchase',
        supplierId: 'SUP-002',
        orderDate: '2024-09-01',
        deliveryDate: '2024-09-10',
        quantity: 75,
        totalPrice: 974.25,
        status: 'received',
        selected: false,
        notes: 'Delivered at: 2024-09-10T09:15:00Z',
      },
    ];
  }
}