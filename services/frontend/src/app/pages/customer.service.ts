import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { Customer, CustomerSegment } from '../models/customer.model';
import { BackendCustomer, BackendCustomerResponse } from '../models/backend.model';

@Injectable({
  providedIn: 'root',
})
export class CustomerService {
  private httpClient = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8000/api/v1';

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

  private extractNumericId(id: string): string | null {
    // Handle customer IDs
    if (id.startsWith('CUST-')) {
      const numPart = id.substring(5);
      const numericValue = parseInt(numPart, 10);
      return numericValue.toString();
    }
    return id.match(/^\d+$/) ? id : null;
  }

  private getFallbackCustomers(): Customer[] {
    return [
      {
        id: 'CUST-001',
        name: 'Sunrise CafÃ© Chain',
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
}