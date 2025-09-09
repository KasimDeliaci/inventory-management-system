import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CampaignListingComponent } from './campaign-listing';

describe('CampaignListing', () => {
  let component: CampaignListingComponent;
  let fixture: ComponentFixture<CampaignListingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampaignListingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CampaignListingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
