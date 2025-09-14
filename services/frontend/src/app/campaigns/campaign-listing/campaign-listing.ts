import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Campaign } from '../../models/campaign.model';
import { CampaignComponent } from '../campaign/campaign';

@Component({
  selector: 'app-campaign-listing',
  standalone: true,
  imports: [CommonModule, CampaignComponent],
  templateUrl: './campaign-listing.html',
  styleUrls: ['./campaign-listing.scss']
})
export class CampaignListingComponent {
  @Input({ required: true }) campaigns: Campaign[] = [];
  @Output() edit = new EventEmitter<Campaign>();
  @Output() selectionChange = new EventEmitter<void>();

  get allSelected(): boolean {
    return this.campaigns.length > 0 && this.campaigns.every(c => c.selected);
  }

  get someSelected(): boolean {
    return this.campaigns.some(c => c.selected) && !this.allSelected;
  }

  toggleAll(checked: boolean) {
    for (const c of this.campaigns) {
      c.selected = checked;
    }
    this.selectionChange.emit();
  }
}