export type ProductStatus = 'ok' | 'warning' | 'critical';

export interface Product {
id: string;
name: string;
category: string;
unit: string;

description?: string | null;
price?: number | null;
safetyStock?: number | null;
reorderPoint?: number | null;
currentStock?: number | null;

preferredSupplierId?: string | null; // single preferred supplier
activeSupplierIds: string[]; // multiple active suppliers

status: ProductStatus;
selected?: boolean;
}