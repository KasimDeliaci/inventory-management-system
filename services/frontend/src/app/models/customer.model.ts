export type CustomerSegment = 'sme' | 'individual' | 'institutional';

export interface Customer {
  /** Unique identifier for the customer */
  id: string;
  /** Name of the customer */
  name: string;
  /** Customer segment */
  segment: CustomerSegment;
  /** Email address */
  email: string;
  /** Phone number */
  phone: string;
  /** City where customer is located */
  city: string;
  /**
   * Whether the customer is selected in the UI.
   * Optional; defaults to false if not set.
   */
  selected?: boolean;
}