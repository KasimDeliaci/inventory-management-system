export type ProductStatus = 'ok' | 'warning' | 'critical';

export interface Product {
  /** Unique identifier for the product */
  id: string;
  /** Name of the product */
  name: string;
  /** Category to which the product belongs */
  category: string;
  /**
   * Unit of measurement for the product (e.g., "kg", "pcs", "liters").
   * Should be a short string representing the unit.
   */
  unit: string;

  /**
   * Optional description of the product.
   * Can be null or omitted if not available.
   */
  description?: string | null;
  /**
   * Optional price of the product.
   * Can be null or omitted if not set.
   */
  price?: number | null;
  /**
   * Optional safety stock level for the product.
   * Can be null or omitted if not set.
   */
  safetyStock?: number | null;
  /**
   * Optional reorder point for the product.
   * Can be null or omitted if not set.
   */
  reorderPoint?: number | null;
  /**
   * Optional current stock level for the product.
   * Can be null or omitted if not set.
   */
  currentStock?: number | null;

  /**
   * Optional ID of the preferred supplier for this product.
   * Can be null or omitted if there is no preferred supplier.
   */
  preferredSupplierId?: string | null; // single preferred supplier
  /**
   * List of IDs of active suppliers for this product.
   */
  activeSupplierIds: string[]; // multiple active suppliers

  /** Status of the product (e.g., "ok", "warning", "critical") */
  status: ProductStatus;
  /**
   * Whether the product is selected in the UI.
   * Optional; defaults to false if not set.
   */
  selected?: boolean;
}