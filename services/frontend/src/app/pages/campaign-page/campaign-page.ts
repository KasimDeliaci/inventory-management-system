// campaign-page.ts
import { Component, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../shared/header/header';
import { SideNav } from '../../shared/side-nav/side-nav';
import { CampaignListingComponent } from '../../campaigns/campaign-listing/campaign-listing';
import { CampaignEditorComponent } from '../../campaigns/campaign-editor/campaign-editor';
import { Campaign, CampaignType, AssignmentType } from '../../models/campaign.model';
import { Product } from '../../models/product.model';
import { Customer } from '../../models/customer.model';
import { MockDataService } from '../mock-data.service';

@Component({
  selector: 'app-campaign-page',
  standalone: true,
  imports: [
    CommonModule,
    Header,
    SideNav,
    CampaignListingComponent,
    CampaignEditorComponent,
  ],
  templateUrl: './campaign-page.html',
  styleUrls: ['./campaign-page.scss'],
})
export class CampaignPageComponent {
  private mockDataService = inject(MockDataService);

  query = signal('');
  editorOpen = signal(false);
  editing = signal<Campaign | null>(null);
  
  // Filter states
  filterOpen = signal(false);

  private selectionTick = signal(0);
  
  bumpSelection() {
    this.selectionTick.update((n) => n + 1);
  }

  // Initialize with mock data
  private all = signal<Campaign[]>(this.mockDataService.getCampaigns());
  products = signal<Product[]>(this.mockDataService.getProducts());
  customers = signal<Customer[]>(this.mockDataService.getCustomers());

  readonly campaigns = computed(() => {
    let filtered = this.all();
    
    // Apply search query
    const q = this.query().trim().toLowerCase();
    if (q) {
      filtered = filtered.filter((c) =>
        [
          c.id,
          c.name,
          c.type,
          c.assignmentType,
          c.percentage.toString(),
        ].some((v) => v.toLowerCase().includes(q))
      );
    }
    
    return filtered;
  });

  /** Depends on selectionTick so it updates when checkboxes toggle */
  selectedCount = computed(() => {
    this.selectionTick();
    return this.all().filter((c) => c.selected).length;
  });

  // Header event handlers
  onAddCampaign() {
    this.editing.set({
      id: '',
      name: '',
      type: 'discount',
      assignmentType: 'product',
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // 30 days from now
      percentage: 0,
      productIds: [],
      customerIds: [],
      isActive: true,
      selected: false,
    });
    this.editorOpen.set(true);
  }

  onOpenFilters() {
    this.filterOpen.set(!this.filterOpen());
  }

  onClear() {
    this.query.set('');
  }

  deleteSelected() {
    const count = this.selectedCount();
    if (count === 0) return;
    
    const message = `Delete ${count} selected campaign${count > 1 ? 's' : ''}?`;
    if (!confirm(message)) return;
    
    this.all.update((list) => list.filter((c) => !c.selected));
    this.bumpSelection();
  }

  openEditorFor(campaign: Campaign) {
    this.editing.set({ ...campaign });
    this.editorOpen.set(true);
  }

  // Editor event handlers
  handleSave(updated: Campaign) {
    if (updated.id && this.all().some((c) => c.id === updated.id)) {
      // Update existing campaign
      this.all.update((list) => 
        list.map((c) => (c.id === updated.id ? { ...c, ...updated } : c))
      );
    } else {
      // Add new campaign
      const id = updated.id?.trim() || `CAMP-${String(this.all().length + 1).padStart(3, '0')}`;
      this.all.update((list) => [{ ...updated, id, selected: false }, ...list]);
    }
    this.closeEditor();
  }

  handleDelete(id: string) {
    if (confirm('Are you sure you want to delete this campaign?')) {
      this.all.update((list) => list.filter((c) => c.id !== id));
      this.closeEditor();
    }
  }

  closeEditor() {
    this.editorOpen.set(false);
    this.editing.set(null);
  }
}