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

  allSelected = false;

  toggleAll(checked: boolean) {
    this.allSelected = checked;
    for (const c of this.campaigns) c.selected = checked;
    this.selectionChange.emit();
  }
}