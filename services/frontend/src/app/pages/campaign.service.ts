import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, catchError, of } from 'rxjs';
import { AssignmentType, Campaign, CampaignType } from '../models/campaign.model';
import {
  BackendCampaignResponse,
  BackendCustomerSpecialOfferResponse,
  BackendCampaign,
  BackendCustomerSpecialOffer,
} from '../models/backend.model';

@Injectable({
  providedIn: 'root',
})
export class CampaignService {
  private httpClient = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8000/api/v1';

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