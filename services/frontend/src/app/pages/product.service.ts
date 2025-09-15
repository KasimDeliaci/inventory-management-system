import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, catchError, of } from 'rxjs';
import { Product, ProductStatus } from '../models/product.model';
import {
  BackendProduct,
  BackendProductResponse,
  BackendDetailedProduct,
  BackendProductStock,
} from '../models/backend.model';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
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

  // DELETE PRODUCT - New method for backend deletion
  deleteProduct(productId: string): Observable<boolean> {
    const numericId = this.extractNumericId(productId);
    if (!numericId) {
      console.error('Invalid product ID format:', productId);
      return of(false);
    }

    return this.httpClient.delete(`${this.baseUrl}/products/${numericId}`).pipe(
      map(() => true), // Success - return true
      catchError((error) => {
        console.error('Error deleting product:', error);
        return of(false); // Failure - return false
      })
    );
  }

  // DELETE MULTIPLE PRODUCTS - For bulk deletion
  deleteMultipleProducts(productIds: string[]): Observable<{ success: string[], failed: string[] }> {
    const deleteRequests = productIds.map(id => 
      this.deleteProduct(id).pipe(
        map(success => ({ id, success }))
      )
    );

    return forkJoin(deleteRequests).pipe(
      map(results => ({
        success: results.filter(r => r.success).map(r => r.id),
        failed: results.filter(r => !r.success).map(r => r.id)
      })),
      catchError((error) => {
        console.error('Error in bulk delete operation:', error);
        return of({ success: [], failed: productIds });
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
}