import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, catchError, of } from 'rxjs';
import { Product, ProductStatus } from '../models/product.model';
import { Supplier } from '../models/supplier.model';
import { Customer, CustomerSegment } from '../models/customer.model';
import { Order } from '../models/order.model';
import { Campaign } from '../models/campaign.model';
import {
  BackendProduct,
  BackendProductResponse,
  BackendSupplierResponse,
  BackendCustomer,
  BackendCustomerResponse,
  BackendDetailedProduct,
  BackendProductStock,
} from '../models/backend.model';

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

  // For now, these still use mock data - we'll extend them later
  getOrders(): Observable<Order[]> {
    // TODO: Replace with actual backend call
    return of(this.getFallbackOrders());
  }

  getCampaigns(): Observable<Campaign[]> {
    // TODO: Replace with actual backend call
    return of(this.getFallbackCampaigns());
  }

  // Fallback data methods (keeping existing mock data)
  private getFallbackProducts(): Product[] {
    return [
      {
        id: 'ID-001',
        name: 'Lego Star*War edition',
        category: 'Toys',
        unit: 'pcs',
        currentStock: 1000000,
        preferredSupplierId: 'SUP-008',
        activeSupplierIds: ['SUP-003', 'SUP-001', 'SUP-008'],
        status: 'ok',
      },
      {
        id: 'ID-002',
        name: 'Alpha',
        category: 'Chemical',
        unit: 'L',
        currentStock: 2000,
        preferredSupplierId: 'SUP-002',
        activeSupplierIds: [],
        status: 'ok',
      },
    ];
  }

  private getFallbackSuppliers(): Supplier[] {
    return [
      {
        id: 'SUP-001',
        name: 'Acme Co.',
        email: 'sales@acme.com',
        phone: '+90 555 111 22 33',
        city: 'Ankara',
      },
      {
        id: 'SUP-002',
        name: 'Globex',
        email: 'info@globex.io',
        phone: '+90 555 222 33 44',
        city: 'Istanbul',
      },
      {
        id: 'SUP-003',
        name: 'Initech',
        email: 'hello@initech.com',
        phone: '+90 555 333 44 55',
        city: 'Izmir',
      },
      {
        id: 'SUP-008',
        name: 'TechCorp',
        email: 'contact@techcorp.com',
        phone: '+90 555 444 55 66',
        city: 'Bursa',
      },
    ];
  }

  private getFallbackCustomers(): Customer[] {
    return [
      {
        id: 'CUST-001',
        name: 'Alice Johnson',
        segment: 'individual',
        email: 'alice.johnson@example.com',
        phone: '+90 501 111 22 33',
        city: 'Ankara',
      },
      {
        id: 'CUST-002',
        name: 'Bob Smith',
        segment: 'sme',
        email: 'bob.smith@example.com',
        phone: '+90 502 222 33 44',
        city: 'Istanbul',
      },
      {
        id: 'CUST-003',
        name: 'Clara Lee',
        segment: 'institutional',
        email: 'clara.lee@example.com',
        phone: '+90 503 333 44 55',
        city: 'Izmir',
      },
      {
        id: 'CUST-004',
        name: 'David Brown',
        segment: 'sme',
        email: 'david.brown@example.com',
        phone: '+90 504 444 55 66',
        city: 'Bursa',
      },
      {
        id: 'CUST-005',
        name: 'Eva Green',
        segment: 'individual',
        email: 'eva.green@example.com',
        phone: '+90 505 555 66 77',
        city: 'Antalya',
      },
    ];
  }

  private getFallbackOrders(): Order[] {
    return [
      {
        id: 'PO-001',
        type: 'purchase',
        supplierId: 'SUP-001',
        orderDate: '2024-09-01',
        deliveryDate: '2024-09-15',
        quantity: 500,
        totalPrice: 25000,
        status: 'received',
        productId: 'ID-001',
      },
      {
        id: 'SO-001',
        type: 'sales',
        customerId: 'CUST-001',
        orderDate: '2024-09-02',
        deliveryDate: '2024-09-16',
        quantity: 10,
        totalPrice: 2500,
        status: 'delivered',
        productId: 'ID-012',
      },
    ];
  }

  private getFallbackCampaigns(): Campaign[] {
    return [
      {
        id: 'CAMP-001',
        name: 'Spring Sale',
        type: 'seasonal',
        assignmentType: 'product',
        startDate: '2024-03-01',
        endDate: '2024-03-31',
        percentage: 20,
        productIds: ['ID-001', 'ID-012', 'ID-016'],
        customerIds: [],
        description: 'Spring seasonal discount on selected products',
        isActive: true,
      },
    ];
  }
}
