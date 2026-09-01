import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { TrackingService } from '../../shared/services/tracking.service';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-trends',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  templateUrl: './trends.component.html',
  styleUrl: './trends.component.scss'
})
export class TrendsComponent implements OnInit {

  loading = true;
  error = '';
  campaigns: any[] = [];
  chartData: any = null;
  hoveredIndex: number | null = null;

  constructor(
    private trackingService: TrackingService,
    public authService: AuthService
  ) {}

  ngOnInit() {
    this.loadTrends();
  }

  loadTrends() {
    this.loading = true;
    this.trackingService.getGlobalDashboard().subscribe({
      next: (data) => {
        this.campaigns = data.campaigns || [];
        this.prepareChartData();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des tendances';
        this.loading = false;
        console.error(err);
      }
    });
  }

  prepareChartData() {
    const sorted = [...this.campaigns]
      .filter(c => c.createdAt)
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

    if (sorted.length === 0) {
      this.chartData = null;
      return;
    }

    const labels = sorted.map(c => c.name.length > 18 ? c.name.substring(0, 18) + '...' : c.name);
    const openRates = sorted.map(c => c.openRate || 0);
    const clickRates = sorted.map(c => c.clickRate || 0);
    const submitRates = sorted.map(c => c.submitRate || 0);

    const avgOpen = openRates.reduce((a, b) => a + b, 0) / openRates.length;
    const avgClick = clickRates.reduce((a, b) => a + b, 0) / clickRates.length;
    const avgSubmit = submitRates.reduce((a, b) => a + b, 0) / submitRates.length;

    const maxOpen = Math.max(...openRates);
    const maxClick = Math.max(...clickRates);
    const maxSubmit = Math.max(...submitRates);
    const minOpen = Math.min(...openRates);
    const minClick = Math.min(...clickRates);
    const minSubmit = Math.min(...submitRates);

    const maxValue = Math.max(
      ...openRates, ...clickRates, ...submitRates, 10
    );

    this.chartData = {
      labels,
      openRates,
      clickRates,
      submitRates,
      avgOpen,
      avgClick,
      avgSubmit,
      maxOpen,
      maxClick,
      maxSubmit,
      minOpen,
      minClick,
      minSubmit,
      maxValue: Math.ceil(maxValue / 10) * 10
    };
  }

  getLatestTrend(): string {
    if (!this.chartData || this.chartData.openRates.length === 0) return 'N/A';
    const latest = this.chartData.openRates[this.chartData.openRates.length - 1];
    const avg = this.chartData.avgOpen;
    if (latest > avg * 1.05) return 'En hausse';
    if (latest < avg * 0.95) return 'En baisse';
    return 'Stable';
  }

  getLatestTrendColor(): string {
    if (!this.chartData || this.chartData.openRates.length === 0) return '#64748b';
    const latest = this.chartData.openRates[this.chartData.openRates.length - 1];
    const avg = this.chartData.avgOpen;
    if (latest > avg * 1.05) return '#10b981';
    if (latest < avg * 0.95) return '#ef4444';
    return '#64748b';
  }

  getMaxValue(): number {
    return this.chartData?.maxValue || 10;
  }

  getBarHeight(value: number): number {
    const max = this.getMaxValue();
    return max > 0 ? (value / max) * 100 : 0;
  }
}