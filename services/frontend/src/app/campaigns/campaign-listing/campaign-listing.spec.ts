import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CampaignListing } from './campaign-listing';

describe('CampaignListing', () => {
  let component: CampaignListing;
  let fixture: ComponentFixture<CampaignListing>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampaignListing]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CampaignListing);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
