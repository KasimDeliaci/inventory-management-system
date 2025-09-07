export interface Supplier {
  id: string;
  name: string;
  email: string;
  phone: string;
  city: string;
  /**
   * Whether the supplier is selected in the UI.
   * Optional; defaults to false if not set.
   */
  selected?: boolean;
}