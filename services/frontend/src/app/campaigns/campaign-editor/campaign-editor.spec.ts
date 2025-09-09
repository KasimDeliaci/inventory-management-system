import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CampaignEditorComponent } from './campaign-editor';

describe('CampaignEditor', () => {
  let component: CampaignEditorComponent;
  let fixture: ComponentFixture<CampaignEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampaignEditorComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CampaignEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
