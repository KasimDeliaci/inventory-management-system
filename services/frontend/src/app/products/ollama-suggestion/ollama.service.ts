import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

interface OllamaSuggestionResponse {
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class OllamaService {
  private httpClient = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8200/api/v1';

  /**
   * Get AI suggestion for a product
   * @param productId - The product ID (will be converted to numeric format)
   * @param asOfDate - Date in YYYY-MM-DD format (optional, defaults to today + 7 days)
   * @returns Observable with the suggestion message
   */
  getProductSuggestion(productId: string, asOfDate?: string): Observable<string> {
    const numericId = this.extractNumericId(productId);
    if (!numericId) {
      console.error('Invalid product ID format:', productId);
      return of('Invalid product ID format');
    }

    // Default to 7 days from now if no date provided
    const targetDate = asOfDate || this.getDefaultAsOfDate();
    
    const url = `${this.baseUrl}/plan`;
    const params = {
      productId: numericId,
      asOfDate: targetDate
    };

    return this.httpClient.get<OllamaSuggestionResponse>(url, { params }).pipe(
      map(response => response.message || 'No suggestion available'),
      catchError((error) => {
        console.error('Error fetching Ollama suggestion:', error);
        return of('Unable to load AI suggestion at this time.');
      })
    );
  }

  /**
   * Extract numeric ID from formatted ID (e.g., "ID-001" -> "1001")
   */
  private extractNumericId(id: string): string | null {
    if (id.startsWith('ID-')) {
      const numPart = id.substring(3);
      const numericValue = parseInt(numPart, 10);
      return (numericValue).toString();
    }
    // If it's already numeric, return as is
    return id.match(/^\d+$/) ? id : null;
  }

  /**
   * Get default asOfDate (fixed to 2025-06-30)
   */
  private getDefaultAsOfDate(): string {
    return '2025-06-30';
  }
}
