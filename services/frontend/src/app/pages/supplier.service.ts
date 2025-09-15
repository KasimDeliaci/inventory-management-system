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

  createSupplier(supplier: Supplier): Observable<Supplier> {
    // Don't include id in the payload - backend will auto-generate it
    const payload = {
      supplierName: supplier.name,
      email: supplier.email,
      phone: supplier.phone,
      city: supplier.city
    };

    console.log('Creating supplier with payload:', payload);

    return this.httpClient.post<any>(`${this.baseUrl}/suppliers`, payload).pipe(
      map((response) => this.transformSingleBackendSupplier(response)),
      catchError((error) => {
        console.error('Error creating supplier:', error);
        throw error;
      })
    );
  }

  updateSupplier(supplier: Supplier): Observable<Supplier> {
    const numericId = this.extractNumericId(supplier.id);
    if (!numericId) {
      console.error('Invalid supplier ID format:', supplier.id);
      throw new Error('Invalid supplier ID format');
    }

    const payload = {
      supplierName: supplier.name,
      email: supplier.email,
      phone: supplier.phone,
      city: supplier.city
    };

    return this.httpClient.put<any>(`${this.baseUrl}/suppliers/${numericId}`, payload).pipe(
      map((response) => this.transformSingleBackendSupplier(response)),
      catchError((error) => {
        console.error('Error updating supplier:', error);
        throw error;
      })
    );
  }

  deleteSupplier(id: string): Observable<void> {
    const numericId = this.extractNumericId(id);
    if (!numericId) {
      console.error('Invalid supplier ID format:', id);
      return of(void 0);
    }

    return this.httpClient.delete<void>(`${this.baseUrl}/suppliers/${numericId}`).pipe(
      catchError((error) => {
        console.error('Error deleting supplier:', error);
        throw error; // Re-throw so caller can handle
      })
    );
  }

  deleteMultipleSuppliers(ids: string[]): Observable<void[]> {
    const deleteRequests = ids.map(id => this.deleteSupplier(id));
    return this.httpClient.request('POST', `${this.baseUrl}/suppliers/batch-delete`, {
      body: { supplierIds: ids.map(id => this.extractNumericId(id)).filter(Boolean) }
    }).pipe(
      map(() => []), // Return empty array on success
      catchError((error) => {
        console.error('Batch delete not supported, falling back to individual deletes');
        // Fallback to individual delete requests
        return this.executeSequentialDeletes(ids);
      })
    );
  }

  private executeSequentialDeletes(ids: string[]): Observable<void[]> {
    return new Observable(observer => {
      const results: void[] = [];
      let completed = 0;
      let hasError = false;

      ids.forEach((id, index) => {
        this.deleteSupplier(id).subscribe({
          next: () => {
            results[index] = void 0;
            completed++;
            if (completed === ids.length && !hasError) {
              observer.next(results);
              observer.complete();
            }
          },
          error: (error) => {
            hasError = true;
            observer.error(error);
          }
        });
      });
    });
  }

  private transformBackendSuppliers(backendSuppliers: any[]): Supplier[] {
    return backendSuppliers.map((backendSupplier) => this.transformSingleBackendSupplier(backendSupplier));
  }

  private transformSingleBackendSupplier(backendSupplier: any): Supplier {
    return {
      id: `SUP-${backendSupplier.supplierId.toString().padStart(3, '0')}`,
      name: backendSupplier.supplierName,
      email: backendSupplier.email,
      phone: this.formatPhoneForDisplay(backendSupplier.phone),
      city: backendSupplier.city,
      selected: false,
    };
  }

  // Format phone number for display (add spaces for readability)
  private formatPhoneForDisplay(phone: string): string {
    if (!phone) return phone;
    
    // Remove all non-digit characters except +
    const cleaned = phone.replace(/[^\d+]/g, '');
    
    // Format Turkish phone numbers: +90 XXX XXX XXXX
    if (cleaned.startsWith('+90') && cleaned.length === 13) {
      return `+90 ${cleaned.slice(3, 6)} ${cleaned.slice(6, 9)} ${cleaned.slice(9)}`;
    }
    
    // Format other international numbers with country codes
    if (cleaned.startsWith('+') && cleaned.length > 10) {
      // Try to detect common patterns
      if (cleaned.length === 14) { // +XX XXX XXX XXXX
        const countryCode = cleaned.slice(0, 3);
        const remaining = cleaned.slice(3);
        return `${countryCode} ${remaining.slice(0, 3)} ${remaining.slice(3, 6)} ${remaining.slice(6)}`;
      } else if (cleaned.length === 12) { // +X XXX XXX XXXX
        const countryCode = cleaned.slice(0, 2);
        const remaining = cleaned.slice(2);
        return `${countryCode} ${remaining.slice(0, 3)} ${remaining.slice(3, 6)} ${remaining.slice(6)}`;
      }
    }
    
    // Return original if no pattern matches
    return phone;
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

  private formatPhoneNumber(phone: string): string {
    if (!phone) return phone;
    
    // Remove all spaces, hyphens, and parentheses but keep the + sign
    return phone.replace(/[\s\-\(\)]/g, '');
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