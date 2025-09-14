import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Campaign } from '../../models/campaign.model';

@Component({
  selector: 'app-campaign',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './campaign.html',
  styleUrls: ['./campaign.scss']
})
export class CampaignComponent {
  @Input({ required: true }) campaign!: Campaign;
  @Output() toggle = new EventEmitter<boolean>();
  @Output() rowClick = new EventEmitter<Campaign>();

  get assignmentDisplay() {
    const count = this.campaign.assignmentType === 'product' 
      ? this.campaign.productIds.length 
      : this.campaign.customerIds.length;
    
    const type = this.campaign.assignmentType === 'product' ? 'products' : 'customers';
    
    return `${count} ${type}`;
  }

  get offerDisplay() {
    if (this.campaign.type === 'discount') {
      return `${this.campaign.percentage || 0}%`;
    } else if (this.campaign.type === 'promotion') {
      const buyQty = this.campaign.buyQty || 0;
      const getQty = this.campaign.getQty || 0;
      return `Buy ${buyQty} Get ${getQty}`;
    } else if (this.campaign.type === 'seasonal') {
      return `${this.campaign.percentage || 0}% Off`;
    } else if (this.campaign.type === 'clearance') {
      return `${this.campaign.percentage || 0}% Off`;
    }
    return '';
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}