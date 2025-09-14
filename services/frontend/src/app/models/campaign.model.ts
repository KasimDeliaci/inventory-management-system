export type CampaignType = 'discount' | 'promotion' | 'seasonal' | 'clearance';
export type AssignmentType = 'product' | 'customer';

export interface Campaign {
  id: string;
  name: string;
  description?: string;
  type: CampaignType;
  assignmentType: AssignmentType;
  percentage: number;
  buyQty?: number | null;
  getQty?: number | null;
  startDate: string;
  endDate: string;
  productIds: string[];
  customerIds: string[];
  isActive: boolean;
  selected: boolean;
}