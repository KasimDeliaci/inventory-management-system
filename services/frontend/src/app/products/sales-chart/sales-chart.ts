import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of, catchError, map, switchMap } from 'rxjs';

// Interfaces for API responses
interface DailySalesData {
  date: string;
  productId: number;
  salesUnits: number;
  offerActiveShare: number;
}

interface WeeklySalesData {
  week: string;
  totalSales: number;
  label: string;
  type: 'historical' | 'forecast';
  confidence?: number;
}

interface ForecastRequest {
  productIds: number[];
  horizonDays: number;
  asOfDate: string;
  returnDaily: true;
}

interface ForecastResponse {
  forecasts: Array<{
    productId: number;
    daily: Array<{
      date: string;
      yhat: number;
    }>;
    sum: number;
    predictionInterval: {
      lowerBound: number;
      upperBound: number;
    };
    confidence: {
      score: number;
      level: string;
      factors: {
        historical_pattern: string;
        seasonal_effects: string;
      };
      recommendation: string;
    };
  }>;
  modelVersion: string;
  modelType: string;
  generatedAt: string;
  forecastId: number;
}

@Component({
  selector: 'app-sales-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sales-chart.html',
  styleUrls: ['./sales-chart.scss']
})
export class SalesChartComponent implements OnInit, OnChanges {
  @Input() productId: string | null = null;

  private httpClient = inject(HttpClient);
  private destroyRef = inject(DestroyRef);

  loading = signal(false);
  private rawChartData = signal<WeeklySalesData[]>([]);
  
  // Today's date set to 2025-06-30 as specified
  private readonly TODAY = new Date('2025-06-30');

  chartData = computed(() => this.rawChartData());

  ngOnInit() {
    if (this.productId) {
      this.loadChartData();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['productId'] && this.productId) {
      this.loadChartData();
    }
  }

  private loadChartData() {
    if (!this.productId) return;

    this.loading.set(true);
    
    // Calculate date ranges
    const weeks = this.calculateWeekRanges();
    
    // Get historical sales data for the past 3 weeks
    const historicalRequests = weeks.historical.map(week => 
      this.getProductSalesData(this.productId!, week.start, week.end).pipe(
        map(salesData => ({
          week: `${week.start}_${week.end}`,
          totalSales: salesData.reduce((sum, day) => sum + (day.salesUnits || 0), 0),
          label: week.label,
          type: 'historical' as const
        }))
      )
    );

    // Get forecast data for the next 2 weeks
    const forecastRequests = weeks.forecast.map(week => 
      this.getForecastData(this.productId!, week.horizonDays).pipe(
        map(forecastData => ({
          week: `forecast_${week.horizonDays}`,
          totalSales: forecastData?.sum || 0,
          label: week.label,
          type: 'forecast' as const,
          confidence: forecastData?.confidence || 0
        }))
      )
    );

    // Combine all requests
    const allRequests = [...historicalRequests, ...forecastRequests];
    
    const subscription = forkJoin(allRequests).subscribe({
      next: (results) => {
        // Sort results to ensure proper order: 3 weeks ago -> 2 weeks ago -> last week -> next week -> 2 weeks later
        const sortedResults = this.sortWeeklyData(results);
        this.rawChartData.set(sortedResults);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading chart data:', err);
        this.loading.set(false);
      }
    });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
    });
  }

  private calculateWeekRanges() {
    const historical = [];
    const forecast = [];

    // Historical weeks (3 weeks ago to last week)
    for (let i = 0; i < 3; i++) {
      const weekEnd = new Date(this.TODAY);
      weekEnd.setDate(this.TODAY.getDate() - (7 * (i + 1))); // Go back 1, 2, 3 weeks
      weekEnd.setDate(weekEnd.getDate() - weekEnd.getDay() + 6); // End on Saturday
      
      const weekStart = new Date(weekEnd);
      weekStart.setDate(weekEnd.getDate() - 6); // Start on Sunday
      
      historical.push({
        start: this.formatDate(weekStart),
        end: this.formatDate(weekEnd),
        label: i === 0 ? 'Last Week' : i === 1 ? '2 Weeks Ago' : '3 Weeks Ago'
      });
    }

    // Forecast weeks (next week and 2 weeks later)
    forecast.push({
      horizonDays: 7,
      label: 'Next Week'
    });
    forecast.push({
      horizonDays: 14,
      label: '2 Weeks Later'
    });

    return {
      historical: historical.reverse(), // Show oldest to newest
      forecast
    };
  }

  private getProductSalesData(productId: string, fromDate: string, toDate: string): Observable<DailySalesData[]> {
    const numericId = this.extractNumericId(productId);
    if (!numericId) {
      console.error('Invalid product ID format:', productId);
      return of([]);
    }

    const url = 'http://localhost:8000/api/v1/reporting/product-day-sales';
    const params = new HttpParams()
      .set('productId', numericId)
      .set('from', fromDate)
      .set('to', toDate);

    return this.httpClient.get<DailySalesData[]>(url, { params }).pipe(
      catchError((error) => {
        console.error('Error fetching product sales data:', error);
        return of([]);
      })
    );
  }

  private getForecastData(productId: string, horizonDays: number): Observable<{sum: number, confidence: number} | null> {
    const numericId = this.extractNumericId(productId);
    if (!numericId) {
      console.error('Invalid product ID format:', productId);
      return of(null);
    }

    const payload: ForecastRequest = {
      productIds: [parseInt(numericId, 10)],
      horizonDays,
      asOfDate: this.formatDate(this.TODAY),
      returnDaily: true
    };

    return this.httpClient.post<ForecastResponse>('http://localhost:8100/forecast', payload).pipe(
      switchMap(response => {
        const forecast = response.forecasts.find(f => f.productId === parseInt(numericId, 10));
        if (forecast) {
          // For 2 weeks later, we need to subtract the first week's forecast
          if (horizonDays === 14) {
            // Get the first week's forecast to subtract
            return this.getForecastData(productId, 7).pipe(
              map(firstWeekData => ({
                sum: forecast.sum - (firstWeekData?.sum || 0),
                confidence: forecast.confidence.score
              }))
            );
          } else {
            return of({
              sum: forecast.sum,
              confidence: forecast.confidence.score
            });
          }
        }
        return of(null);
      }),
      catchError((error) => {
        console.error('Error fetching forecast data:', error);
        return of(null);
      })
    );
  }

  private sortWeeklyData(data: WeeklySalesData[]): WeeklySalesData[] {
    const historical = data.filter(d => d.type === 'historical');
    const forecast = data.filter(d => d.type === 'forecast');
    
    // Historical data should already be in correct order from calculateWeekRanges
    // Forecast data should be in order: next week, then 2 weeks later
    return [...historical, ...forecast];
  }

  private extractNumericId(id: string): string | null {
    if (id.startsWith('ID-')) {
      const numPart = id.substring(3);
      const numericValue = parseInt(numPart, 10);
      return numericValue.toString();
    }
    return id.match(/^\d+$/) ? id : null;
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  getBarHeight(value: number): number {
    if (!this.chartData().length) return 0;
    
    const maxValue = Math.max(...this.chartData().map(d => d.totalSales));
    if (maxValue === 0) return 0;
    
    return Math.max(5, (value / maxValue) * 100); // Minimum 5% height for visibility
  }

  getBarTooltip(week: WeeklySalesData): string {
    if (week.type === 'historical') {
      return `${week.label}: ${Math.round(week.totalSales)} units sold`;
    } else {
      return `${week.label}: ${Math.round(week.totalSales)} units predicted (${week.confidence}% confidence)`;
    }
  }

  // Expose Math to template
  Math = Math;
}