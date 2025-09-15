import { Component, Input, OnInit, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../pages/product.service';

interface WeeklySalesData {
  week: string;
  totalSales: number;
  label: string;
}

@Component({
  selector: 'app-sales-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sales-chart.html',
  styleUrls: ['./sales-chart.scss'],
})
export class SalesChartComponent implements OnInit, OnChanges {
  @Input() productId: string | null = null;
  
  private productService = inject(ProductService);
  
  loading = signal(false);
  salesData = signal<WeeklySalesData[]>([]);

  ngOnInit() {
    if (this.productId) {
      this.loadSalesData();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['productId'] && this.productId) {
      this.loadSalesData();
    }
  }

  private loadSalesData() {
    if (!this.productId) return;
    
    this.loading.set(true);
    this.productService.getWeeklySalesData(this.productId).subscribe({
      next: (data) => {
        this.salesData.set(data);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading sales data:', error);
        this.salesData.set([]);
        this.loading.set(false);
      }
    });
  }

  getBarHeight(value: number): number {
    const data = this.salesData();
    if (data.length === 0) return 0;
    
    const maxValue = Math.max(...data.map(d => d.totalSales));
    if (maxValue === 0) return 4; // Minimum height
    
    const percentage = (value / maxValue) * 90; // Max 90% to leave room for labels
    return Math.max(percentage, 4); // Minimum 4% height
  }
}