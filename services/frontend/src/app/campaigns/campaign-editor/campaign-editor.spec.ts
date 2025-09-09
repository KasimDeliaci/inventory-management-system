import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CampaignEditor } from './campaign-editor';

describe('CampaignEditor', () => {
  let component: CampaignEditor;
  let fixture: ComponentFixture<CampaignEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampaignEditor]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CampaignEditor);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
