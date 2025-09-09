import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CampaignPageComponent } from './campaign-page';

describe('CampaignPage', () => {
  let component: CampaignPageComponent;
  let fixture: ComponentFixture<CampaignPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampaignPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CampaignPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
