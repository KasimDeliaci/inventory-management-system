import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../shared/header/header';
import { SideNav } from '../../shared/side-nav/side-nav';
import { CampaignListingComponent } from '../../campaigns/campaign-listing/campaign-listing';
import { CampaignEditorComponent } from '../../campaigns/campaign-editor/campaign-editor';
import { Campaign, CampaignType, AssignmentType } from '../../models/campaign.model';
import { Product } from '../../models/product.model';
import { Customer } from '../../models/customer.model';
import { DataService } from '../data.service';

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
export class CampaignPageComponent implements OnInit {
  private dataService = inject(DataService);

  query = signal('');
  editorOpen = signal(false);
  editing = signal<Campaign | null>(null);
  
  // Filter states
  filterOpen = signal(false);
  typeFilter = signal<CampaignType | 'all'>('all');
  assignmentFilter = signal<AssignmentType | 'all'>('all');
  activeFilter = signal<'active' | 'inactive' | 'all'>('all');

  private selectionTick = signal(0);
  
  bumpSelection() {
    this.selectionTick.update((n) => n + 1);
  }

  // Initialize with empty arrays - data will be loaded in ngOnInit
  private all = signal<Campaign[]>([]);
  products = signal<Product[]>([]);
  customers = signal<Customer[]>([]);

  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    // Load campaigns
    this.dataService.getCampaigns().subscribe({
      next: (campaigns) => {
        this.all.set(campaigns);
      },
      error: (error) => {
        console.error('Error loading campaigns:', error);
      }
    });

    // Load products
    this.dataService.getProducts().subscribe({
      next: (products) => {
        this.products.set(products);
      },
      error: (error) => {
        console.error('Error loading products:', error);
      }
    });

    // Load customers
    this.dataService.getCustomers().subscribe({
      next: (customers) => {
        this.customers.set(customers);
      },
      error: (error) => {
        console.error('Error loading customers:', error);
      }
    });
  }

  readonly campaigns = computed(() => {
    let filtered = this.all();
    
    // Apply type filter
    const typeFilter = this.typeFilter();
    if (typeFilter !== 'all') {
      filtered = filtered.filter(c => c.type === typeFilter);
    }
    
    // Apply assignment filter
    const assignmentFilter = this.assignmentFilter();
    if (assignmentFilter !== 'all') {
      filtered = filtered.filter(c => c.assignmentType === assignmentFilter);
    }
    
    // Apply active filter
    const activeFilter = this.activeFilter();
    if (activeFilter !== 'all') {
      filtered = filtered.filter(c => {
        if (activeFilter === 'active') {
          return c.isActive;
        } else {
          return !c.isActive;
        }
      });
    }
    
    // Apply search query
    const q = this.query().trim().toLowerCase();
    if (q) {
      filtered = filtered.filter((c) =>
        [
          c.id,
          c.name,
          c.type,
          c.assignmentType,
          c.percentage?.toString() || '',
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
      description: '', // Keep empty since we removed description field
      type: 'discount',
      assignmentType: 'product',
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // 30 days from now
      percentage: 0,
      buyQty: null,
      getQty: null,
      productIds: [], // Will be populated from single selection
      customerIds: [], // Will be populated from single selection
      isActive: true,
      selected: false,
    });
    this.editorOpen.set(true);
  }

  onOpenFilters() {
    this.filterOpen.set(!this.filterOpen());
  }

  onAssignmentFilterSelect(assignment: AssignmentType | 'all') {
    this.assignmentFilter.set(assignment);
    // Reset type filter when changing assignment to non-product-based
    if (assignment !== 'product') {
      this.typeFilter.set('all');
    }
    this.filterOpen.set(false);
  }

  onTypeFilterSelect(type: CampaignType | 'all') {
    this.typeFilter.set(type);
    this.filterOpen.set(false);
  }

  onActiveFilterSelect(active: 'active' | 'inactive' | 'all') {
    this.activeFilter.set(active);
    this.filterOpen.set(false);
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