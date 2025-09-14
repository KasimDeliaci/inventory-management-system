import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { Supplier } from '../models/supplier.model';
import { BackendSupplierResponse } from '../models/backend.model';

@Injectable({
  providedIn: 'root',
})
export class SupplierService {
  private httpClient = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8000/api/v1';

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

  private extractNumericId(id: string): string | null {
    // Handle supplier IDs
    if (id.startsWith('SUP-')) {
      const numPart = id.substring(4);
      const numericValue = parseInt(numPart, 10);
      return numericValue.toString();
    }
    return id.match(/^\d+$/) ? id : null;
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
}