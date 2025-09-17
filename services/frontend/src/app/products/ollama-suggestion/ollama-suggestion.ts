import { Component, Input, OnInit, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OllamaService } from './ollama.service';

@Component({
  selector: 'app-ollama-suggestion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ollama-suggestion.html',
  styleUrls: ['./ollama-suggestion.scss'],
})
export class OllamaSuggestion implements OnInit, OnChanges {
  @Input() productId: string | null = null;
  
  private ollamaService = inject(OllamaService);
  
  loading = signal(false);
  suggestion = signal<string | null>(null);
  hasError = signal(false);

  ngOnInit() {
    this.loadSuggestion();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['productId']) {
      this.loadSuggestion();
    }
  }

  private loadSuggestion() {
    // Reset state
    this.suggestion.set(null);
    this.hasError.set(false);
    
    // Don't load for new products (no ID)
    if (!this.productId) {
      return;
    }

    this.loading.set(true);
    
    this.ollamaService.getProductSuggestion(this.productId).subscribe({
      next: (message) => {
        this.suggestion.set(message);
        this.loading.set(false);
        this.hasError.set(false);
      },
      error: (error) => {
        console.error('Error loading suggestion:', error);
        this.suggestion.set(null);
        this.hasError.set(true);
        this.loading.set(false);
      }
    });
  }
}