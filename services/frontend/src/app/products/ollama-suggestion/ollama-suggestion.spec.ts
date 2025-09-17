import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OllamaSuggestion } from './ollama-suggestion';

describe('OllamaSuggestion', () => {
  let component: OllamaSuggestion;
  let fixture: ComponentFixture<OllamaSuggestion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OllamaSuggestion]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OllamaSuggestion);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
