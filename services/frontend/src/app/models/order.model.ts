// order.model.ts
export type OrderType = 'purchase' | 'sales';

export type PurchaseOrderStatus = 'placed' | 'in_transit' | 'received' | 'canceled';
export type SalesOrderStatus = 'pending' | 'allocated' | 'in_transit' | 'delivered' | 'canceled';

export interface Order {
  id: string;
  type: OrderType;
  supplierId?: string; // for purchase orders
  customerId?: string; // for sales orders
  orderDate: string;
  deliveryDate?: string;
  quantity: number;
  totalPrice: number;
  status: PurchaseOrderStatus | SalesOrderStatus;
  selected?: boolean;
  
  // Additional fields
  productId?: string;
  notes?: string;
}

export interface OrderItem {
  id: string;
  orderId: string;
  productId: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

// Type guards
export function isPurchaseOrder(order: Order): order is Order & { status: PurchaseOrderStatus } {
  return order.type === 'purchase';
}

export function isSalesOrder(order: Order): order is Order & { status: SalesOrderStatus } {
  return order.type === 'sales';
}

// Status progression helpers
export const PURCHASE_STATUS_ORDER: PurchaseOrderStatus[] = ['placed', 'in_transit', 'received', 'canceled'];
export const SALES_STATUS_ORDER: SalesOrderStatus[] = ['pending', 'allocated', 'in_transit', 'delivered', 'canceled'];

export function getStatusColor(status: PurchaseOrderStatus | SalesOrderStatus): string {
  if (status === 'canceled') {
    return '#d64545'; // red
  }
  
  const statusOrder = PURCHASE_STATUS_ORDER.includes(status as PurchaseOrderStatus) 
    ? PURCHASE_STATUS_ORDER 
    : SALES_STATUS_ORDER;
  
  const index = statusOrder.indexOf(status as any);
  const total = statusOrder.length - 1; // exclude 'canceled'
  
  if (index === -1 || index === total) return '#d64545'; // canceled or not found
  
  // Interpolate from blue to green
  const ratio = index / (total - 1);
  const blue = Math.round(255 * (1 - ratio));
  const green = Math.round(255 * ratio);
  
  return `rgb(${Math.round(blue * 0.2)}, ${green}, ${Math.round(blue * 0.8)})`;
}