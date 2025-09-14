import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, catchError, of, switchMap, from, mergeMap, toArray } from 'rxjs';
import { Product, ProductStatus } from '../models/product.model';
import { Supplier } from '../models/supplier.model';
import { Customer, CustomerSegment } from '../models/customer.model';
import { Order, OrderType, PurchaseOrderStatus, SalesOrderStatus } from '../models/order.model';
import { AssignmentType, Campaign, CampaignType } from '../models/campaign.model';
import {
  BackendProduct,
  BackendProductResponse,
  BackendSupplierResponse,
  BackendCustomer,
  BackendCustomerResponse,
  BackendDetailedProduct,
  BackendProductStock,
  BackendCampaignResponse,
  BackendCustomerSpecialOfferResponse,
  BackendCampaign,
  BackendCustomerSpecialOffer,
  BackendSalesOrder,
  BackendPurchaseOrder,
  BackendSalesOrderResponse,
  BackendPurchaseOrderResponse,
} from '../models/backend.model';

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
export class DataService {
  private httpClient = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8000/api/v1';

  // INITIAL PAGE LOAD - Only fetch products summary
  getProducts(): Observable<Product[]> {
    return this.httpClient.get<BackendProductResponse>(`${this.baseUrl}/products`).pipe(
      map((response) => this.transformBackendProducts(response.content)),
      catchError((error) => {
        console.error('Error fetching products:', error);
        return of(this.getFallbackProducts());
      })
    );
  }

  private transformBackendProducts(backendProducts: BackendProduct[]): Product[] {
    return backendProducts.map((backendProduct) => ({
      id: `ID-${backendProduct.productId.toString().padStart(3, '0')}`,
      name: backendProduct.productName,
      category: backendProduct.category,
      unit: backendProduct.unitOfMeasure,
      description: null, // Not available in summary endpoint - will be fetched on click
      price: null, // Not available in summary endpoint - will be fetched on click
      safetyStock: null, // Not available in summary endpoint - will be fetched on click
      reorderPoint: null, // Not available in summary endpoint - will be fetched on click
      currentStock: backendProduct.quantityAvailable,
      preferredSupplierId: backendProduct.preferredSupplier
        ? `SUP-${backendProduct.preferredSupplier.supplierId.toString().padStart(3, '0')}`
        : null,
      activeSupplierIds: backendProduct.activeSuppliers.map(
        (s) => `SUP-${s.supplierId.toString().padStart(3, '0')}`
      ),
      status: this.mapInventoryStatusToProductStatus(backendProduct.inventoryStatus),
      selected: false,
    }));
  }

  private mapInventoryStatusToProductStatus(
    inventoryStatus: 'RED' | 'YELLOW' | 'GREEN'
  ): ProductStatus {
    switch (inventoryStatus) {
      case 'RED':
        return 'critical';
      case 'YELLOW':
        return 'warning';
      case 'GREEN':
        return 'ok';
      default:
        return 'ok';
    }
  }

  // PRODUCT CLICK - Fetch detailed product info and stock data
  getProductById(productId: string): Observable<Product | null> {
    const numericId = this.extractNumericId(productId);
    if (!numericId) {
      console.error('Invalid product ID format:', productId);
      return of(null);
    }

    // Only make the detailed API calls when product is clicked
    return forkJoin({
      product: this.httpClient.get<BackendDetailedProduct>(`${this.baseUrl}/products/${numericId}`),
      stock: this.httpClient.get<BackendProductStock>(
        `${this.baseUrl}/products/${numericId}/stock`
      ),
    }).pipe(
      map(({ product, stock }) => this.transformDetailedProduct(product, stock)),
      catchError((error) => {
        console.error('Error fetching detailed product:', error);
        return of(null);
      })
    );
  }

  private transformDetailedProduct(
    product: BackendDetailedProduct,
    stock: BackendProductStock
  ): Product {
    // Transform supplier data from the detailed product response
    const activeSupplierIds = product.activeSuppliers.map(
      (supplier: any) => `SUP-${supplier.supplierId.toString().padStart(3, '0')}`
    );

    const preferredSupplierId = product.preferredSupplier
      ? `SUP-${product.preferredSupplier.supplierId.toString().padStart(3, '0')}`
      : null;

    return {
      id: `ID-${product.productId.toString().padStart(3, '0')}`,
      name: product.productName,
      category: product.category,
      unit: product.unitOfMeasure,
      description: product.description || null,
      price: product.currentPrice || null,
      safetyStock: product.safetyStock || null,
      reorderPoint: product.reorderPoint || null,
      currentStock: stock.quantityAvailable,
      preferredSupplierId,
      activeSupplierIds,
      status: this.determineStatusFromStock(stock, product.safetyStock, product.reorderPoint),
      selected: false,
      // Store additional stock info for tooltip use
      _stockDetails: {
        onHand: stock.quantityOnHand,
        reserved: stock.quantityReserved,
        available: stock.quantityAvailable,
      },
    } as Product & { _stockDetails?: any };
  }

  private determineStatusFromStock(
    stock: BackendProductStock,
    safetyStock: number,
    reorderPoint: number
  ): ProductStatus {
    if (stock.quantityAvailable <= safetyStock) {
      return 'critical';
    } else if (stock.quantityAvailable <= reorderPoint) {
      return 'warning';
    }
    return 'ok';
  }

  private extractNumericId(id: string): string | null {
    // Handle both formats: "ID-001" -> "1" (removes leading zeros)
    if (id.startsWith('ID-')) {
      const numPart = id.substring(3);
      const numericValue = parseInt(numPart, 10);
      return numericValue.toString();
    }
    // Handle supplier IDs
    if (id.startsWith('SUP-')) {
      const numPart = id.substring(4);
      const numericValue = parseInt(numPart, 10);
      return numericValue.toString();
    }
    // Handle customer IDs
    if (id.startsWith('CUST-')) {
      const numPart = id.substring(5);
      const numericValue = parseInt(numPart, 10);
      return numericValue.toString();
    }
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
    return id.match(/^\d+$/) ? id : null;
  }

  getProductStock(productId: string): Observable<BackendProductStock | null> {
    const numericId = this.extractNumericId(productId);
    if (!numericId) {
      console.error('Invalid product ID format:', productId);
      return of(null);
    }

    return this.httpClient
      .get<BackendProductStock>(`${this.baseUrl}/products/${numericId}/stock`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching product stock:', error);
          return of(null);
        })
      );
  }

  // Supplier-related methods
  getSuppliers(): Observable<Supplier[]> {
    return this.httpClient.get<BackendSupplierResponse>(`${this.baseUrl}/suppliers`).pipe(
      map((response) => this.transformBackendSuppliers(response.content)),
      catchError((error) => {
        console.error('Error fetching suppliers:', error);
        return of(this.getFallbackSuppliers());
      })
    );
  }

  private transformBackendSuppliers(backendSuppliers: any[]): Supplier[] {
    return backendSuppliers.map((backendSupplier) => ({
      id: `SUP-${backendSupplier.supplierId.toString().padStart(3, '0')}`,
      name: backendSupplier.supplierName,
      email: backendSupplier.email,
      phone: backendSupplier.phone,
      city: backendSupplier.city,
      selected: false,
    }));
  }

  // Customer-related methods
  getCustomers(): Observable<Customer[]> {
    return this.httpClient.get<BackendCustomerResponse>(`${this.baseUrl}/customers`).pipe(
      map((response) => this.transformBackendCustomers(response.content)),
      catchError((error) => {
        console.error('Error fetching customers:', error);
        return of(this.getFallbackCustomers());
      })
    );
  }

  private transformBackendCustomers(backendCustomers: BackendCustomer[]): Customer[] {
    return backendCustomers.map((backendCustomer) => ({
      id: `CUST-${backendCustomer.customerId.toString().padStart(3, '0')}`,
      name: backendCustomer.customerName,
      segment: this.mapBackendCustomerSegment(backendCustomer.customerSegment),
      email: backendCustomer.email,
      phone: backendCustomer.phone,
      city: backendCustomer.city,
      selected: false,
    }));
  }

  private mapBackendCustomerSegment(
    backendSegment: 'INDIVIDUAL' | 'SME' | 'CORPORATE' | 'ENTERPRISE'
  ): CustomerSegment {
    switch (backendSegment) {
      case 'INDIVIDUAL':
        return 'individual';
      case 'SME':
        return 'sme';
      case 'CORPORATE':
      case 'ENTERPRISE':
        return 'institutional';
      default:
        return 'individual';
    }
  }

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
      products: this.getProducts(),
      orderItems: this.getSalesOrderItems(order.id),
    };

    if (order.customerId) {
      const numericCustomerId = this.extractNumericId(order.customerId);
      if (numericCustomerId) {
        requests['customers'] = this.getCustomers();
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
      products: this.getProducts(),
      orderItems: this.getPurchaseOrderItems(order.id),
    };

    if (order.supplierId) {
      const numericSupplierId = this.extractNumericId(order.supplierId);
      if (numericSupplierId) {
        requests['suppliers'] = this.getSuppliers();
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

  getCampaigns(): Observable<Campaign[]> {
    return forkJoin({
      productCampaigns: this.httpClient
        .get<BackendCampaignResponse>(`${this.baseUrl}/campaigns`)
        .pipe(
          catchError((error) => {
            console.error('Error fetching product campaigns:', error);
            return of({ content: [], page: { page: 0, size: 0, totalElements: 0, totalPages: 0 } });
          })
        ),
      customerCampaigns: this.httpClient
        .get<BackendCustomerSpecialOfferResponse>(`${this.baseUrl}/customer-special-offers`)
        .pipe(
          catchError((error) => {
            console.error('Error fetching customer campaigns:', error);
            return of({ content: [], page: { page: 0, size: 0, totalElements: 0, totalPages: 0 } });
          })
        ),
    }).pipe(
      map(({ productCampaigns, customerCampaigns }) => {
        const productCampaignList = this.transformBackendProductCampaigns(productCampaigns.content);
        const customerCampaignList = this.transformBackendCustomerCampaigns(
          customerCampaigns.content
        );
        return [...productCampaignList, ...customerCampaignList];
      }),
      catchError((error) => {
        console.error('Error fetching campaigns:', error);
        return of(this.getFallbackCampaigns());
      })
    );
  }

  private transformBackendProductCampaigns(backendCampaigns: BackendCampaign[]): Campaign[] {
    return backendCampaigns.map((backendCampaign) => ({
      id: `CAMP-${backendCampaign.campaignId.toString().padStart(3, '0')}`,
      name: backendCampaign.campaignName,
      description: '', // Not available in backend response
      type: this.mapBackendCampaignType(backendCampaign.campaignType),
      assignmentType: 'product' as AssignmentType,
      percentage: backendCampaign.discountPercentage || 0,
      buyQty: backendCampaign.buyQty,
      getQty: backendCampaign.getQty,
      startDate: backendCampaign.startDate,
      endDate: backendCampaign.endDate,
      productIds: backendCampaign.products.map(
        (p) => `ID-${p.productId.toString().padStart(3, '0')}`
      ),
      customerIds: [],
      isActive: this.isCampaignActive(backendCampaign.startDate, backendCampaign.endDate),
      selected: false,
    }));
  }

  private transformBackendCustomerCampaigns(
    backendOffers: BackendCustomerSpecialOffer[]
  ): Campaign[] {
    return backendOffers.map((backendOffer) => ({
      id: `CUST-OFFER-${backendOffer.specialOfferId.toString().padStart(3, '0')}`,
      name: `Customer ${backendOffer.customerId} - ${backendOffer.percentOff}% OFF`,
      description: `Special discount for customer ${backendOffer.customerId}`,
      type: 'discount' as CampaignType,
      assignmentType: 'customer' as AssignmentType,
      percentage: backendOffer.percentOff,
      buyQty: null,
      getQty: null,
      startDate: backendOffer.startDate,
      endDate: backendOffer.endDate,
      productIds: [],
      customerIds: [`CUST-${backendOffer.customerId.toString().padStart(3, '0')}`],
      isActive: this.isCampaignActive(backendOffer.startDate, backendOffer.endDate),
      selected: false,
    }));
  }

  private mapBackendCampaignType(backendType: 'DISCOUNT' | 'BXGY_SAME_PRODUCT'): CampaignType {
    switch (backendType) {
      case 'DISCOUNT':
        return 'discount';
      case 'BXGY_SAME_PRODUCT':
        return 'promotion';
      default:
        return 'discount';
    }
  }

  private isCampaignActive(startDate: string, endDate: string): boolean {
    const now = new Date();
    const start = new Date(startDate);
    const end = new Date(endDate);
    return now >= start && now <= end;
  }

  // FALLBACK DATA METHODS
  
  private getFallbackProducts(): Product[] {
    return [
      {
        id: 'ID-001',
        name: 'Premium Coffee Beans',
        category: 'Beverages',
        unit: 'kg',
        description: 'High-quality arabica coffee beans from Colombia',
        price: 25.99,
        safetyStock: 10,
        reorderPoint: 20,
        currentStock: 5,
        preferredSupplierId: 'SUP-001',
        activeSupplierIds: ['SUP-001', 'SUP-002'],
        status: 'critical',
        selected: false,
      },
      {
        id: 'ID-002',
        name: 'Organic Green Tea',
        category: 'Beverages',
        unit: 'boxes',
        description: 'Premium organic green tea leaves',
        price: 18.50,
        safetyStock: 15,
        reorderPoint: 25,
        currentStock: 22,
        preferredSupplierId: 'SUP-003',
        activeSupplierIds: ['SUP-003'],
        status: 'warning',
        selected: false,
      },
      {
        id: 'ID-003',
        name: 'Stainless Steel Mug',
        category: 'Accessories',
        unit: 'pieces',
        description: 'Durable stainless steel travel mug',
        price: 12.99,
        safetyStock: 20,
        reorderPoint: 30,
        currentStock: 45,
        preferredSupplierId: 'SUP-002',
        activeSupplierIds: ['SUP-001', 'SUP-002'],
        status: 'ok',
        selected: false,
      },
    ];
  }

  private getFallbackSuppliers(): Supplier[] {
    return [
      {
        id: 'SUP-001',
        name: 'Global Coffee Suppliers Ltd.',
        email: 'orders@globalcoffee.com',
        phone: '+1-555-0123',
        city: 'Seattle',
        selected: false,
      },
      {
        id: 'SUP-002',
        name: 'Premium Beverage Distributors',
        email: 'sales@premiumbev.com',
        phone: '+1-555-0456',
        city: 'Portland',
        selected: false,
      },
      {
        id: 'SUP-003',
        name: 'Organic Tea Company',
        email: 'info@organictea.com',
        phone: '+1-555-0789',
        city: 'San Francisco',
        selected: false,
      },
    ];
  }

  private getFallbackCustomers(): Customer[] {
    return [
      {
        id: 'CUST-001',
        name: 'Sunrise Café Chain',
        segment: 'institutional',
        email: 'procurement@sunrisecafe.com',
        phone: '+1-555-1001',
        city: 'New York',
        selected: false,
      },
      {
        id: 'CUST-002',
        name: 'John Smith',
        segment: 'individual',
        email: 'john.smith@email.com',
        phone: '+1-555-1002',
        city: 'Los Angeles',
        selected: false,
      },
      {
        id: 'CUST-003',
        name: 'Local Business Solutions',
        segment: 'sme',
        email: 'orders@localbiz.com',
        phone: '+1-555-1003',
        city: 'Chicago',
        selected: false,
      },
      {
        id: 'CUST-004',
        name: 'Metro Office Complex',
        segment: 'institutional',
        email: 'facilities@metrooffice.com',
        phone: '+1-555-1004',
        city: 'Houston',
        selected: false,
      },
    ];
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

  private getFallbackCampaigns(): Campaign[] {
    return [
      {
        id: 'CAMP-001',
        name: 'Summer Coffee Special',
        description: '20% off all coffee products during summer season',
        type: 'discount',
        assignmentType: 'product',
        percentage: 20,
        buyQty: null,
        getQty: null,
        startDate: '2024-06-01',
        endDate: '2024-08-31',
        productIds: ['ID-001'],
        customerIds: [],
        isActive: false,
        selected: false,
      },
      {
        id: 'CAMP-002',
        name: 'Buy 2 Get 1 Free Tea',
        description: 'Special promotion on organic tea boxes',
        type: 'promotion',
        assignmentType: 'product',
        percentage: 0,
        buyQty: 2,
        getQty: 1,
        startDate: '2024-09-01',
        endDate: '2024-09-30',
        productIds: ['ID-002'],
        customerIds: [],
        isActive: true,
        selected: false,
      },
      {
        id: 'CUST-OFFER-001',
        name: 'VIP Customer Discount',
        description: 'Special 15% discount for premium institutional customers',
        type: 'discount',
        assignmentType: 'customer',
        percentage: 15,
        buyQty: null,
        getQty: null,
        startDate: '2024-01-01',
        endDate: '2024-12-31',
        productIds: [],
        customerIds: ['CUST-001', 'CUST-004'],
        isActive: true,
        selected: false,
      },
      {
        id: 'CAMP-003',
        name: 'Accessory Bundle Deal',
        description: '10% off when buying 5 or more mugs',
        type: 'discount',
        assignmentType: 'product',
        percentage: 10,
        buyQty: 5,
        getQty: null,
        startDate: '2024-09-15',
        endDate: '2024-10-15',
        productIds: ['ID-003'],
        customerIds: [],
        isActive: true,
        selected: false,
      },
      {
        id: 'CUST-OFFER-002',
        name: 'SME Partnership Discount',
        description: '12% discount for small business partners',
        type: 'discount',
        assignmentType: 'customer',
        percentage: 12,
        buyQty: null,
        getQty: null,
        startDate: '2024-07-01',
        endDate: '2024-12-31',
        productIds: [],
        customerIds: ['CUST-003'],
        isActive: true,
        selected: false,
      },
    ];
  }
}