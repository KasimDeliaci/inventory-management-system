// Product backend models - EXAMPLE: http://localhost:8000/api/v1/products
export interface BackendSupplierSmallDetailed {
  supplierId: number;
  supplierName: string;
}

export interface BackendProduct {
  productId: number;
  productName: string;
  category: string;
  unitOfMeasure: string;
  quantityAvailable: number;
  activeSuppliers: BackendSupplierSmallDetailed[];
  preferredSupplier: BackendSupplierSmallDetailed;
  inventoryStatus: 'RED' | 'YELLOW' | 'GREEN';
}

export interface BackendProductResponse {
  content: BackendProduct[];
  page: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// Supplier backend models - EXAMPLE: http://localhost:8000/api/v1/suppliers
export interface BackendSupplier {
  supplierId: number;
  supplierName: string;
  email: string;
  phone: string;
  city: string;
  createdAt: string;
  updatedAt: string;
}

export interface BackendSupplierResponse {
  content: BackendSupplier[];
  page: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// Detailed product data (for individual product calls) - EXAMPLE: http://localhost:8000/api/v1/products/1001
export interface BackendDetailedProduct {
  productId: number;
  productName: string;
  description: string;
  category: string;
  unitOfMeasure: string;
  safetyStock: number;
  reorderPoint: number;
  currentPrice: number;
  activeSuppliers: BackendSupplier[];
  preferredSupplier: BackendSupplier;
}

export interface BackendProductSupplierResponse {
  content: BackendSupplier[];
  page: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// Product stock information (for /products/{id}/stock endpoint) - EXAMPLE: http://localhost:8000/api/v1/products/1001/stock
export interface BackendProductStock {
  productId: number;
  quantityOnHand: number;
  quantityReserved: number;
  quantityAvailable: number;
  lastMovementId: number | null;
  lastUpdated: string;
}

// Customer backend models - EXAMPLE: http://localhost:8000/api/v1/customers
export interface BackendCustomer {
  customerId: number;
  customerName: string;
  customerSegment: 'INDIVIDUAL' | 'SME' | 'CORPORATE' | 'ENTERPRISE';
  email: string;
  phone: string;
  city: string;
  createdAt: string;
  updatedAt: string;
}

export interface BackendCustomerResponse {
  content: BackendCustomer[];
  page: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// Order backend models - EXAMPLE: http://localhost:8000/api/v1/sales-orders and http://localhost:8000/api/v1/purchase-orders
export interface BackendSalesOrder {
  salesOrderId: number;
  customerId: number;
  orderDate: string;
  deliveryDate: string;
  deliveredAt: string | null;
  status: 'PENDING' | 'ALLOCATED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELED';
  customerSpecialOfferId: number | null;
  customerDiscountPctApplied: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface BackendPurchaseOrder {
  purchaseOrderId: number;
  supplierId: number;
  orderDate: string;
  expectedDelivery: string;
  actualDelivery: string | null;
  status: 'PLACED' | 'IN_TRANSIT' | 'RECEIVED' | 'CANCELED';
  createdAt: string;
  updatedAt: string;
}

export interface BackendSalesOrderResponse {
  content: BackendSalesOrder[];
  page: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface BackendPurchaseOrderResponse {
  content: BackendPurchaseOrder[];
  page: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// Campaign backend models - EXAMPLE: http://localhost:8000/api/v1/campaigns
export interface BackendCampaign {
  campaignId: number;
  campaignName: string;
  campaignType: 'DISCOUNT' | 'BXGY_SAME_PRODUCT';
  discountPercentage: number | null;
  buyQty: number | null;
  getQty: number | null;
  startDate: string;
  endDate: string;
  products: BackendProduct[];
  createdAt: string;
  updatedAt: string;
}

export interface BackendCampaignResponse {
  content: BackendCampaign[];
  page: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface BackendCustomerSpecialOffer {
  specialOfferId: number;
  customerId: number;
  percentOff: number;
  startDate: string;
  endDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface BackendCustomerSpecialOfferResponse {
  content: BackendCustomerSpecialOffer[];
  page: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}