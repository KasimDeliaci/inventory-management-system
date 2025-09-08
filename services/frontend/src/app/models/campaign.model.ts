export type CampaignType = 'discount' | 'promotion' | 'seasonal' | 'clearance';
export type AssignmentType = 'product' | 'customer';

export interface Campaign {
  id: string;
  name: string;
  type: CampaignType;
  assignmentType: AssignmentType;
  startDate: string; 
  endDate: string; 
  percentage: number; // discount percentage
  
  // Assignment targets
  productIds: string[]; // assigned product IDs
  customerIds: string[]; // assigned customer IDs
  
  description?: string;
  isActive?: boolean;
  selected?: boolean;
}