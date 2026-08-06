import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { TrackingService } from '../../shared/services/tracking.service';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-campaign-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  templateUrl: './campaign-detail.component.html'
})
export class CampaignDetailComponent implements OnInit {

  campaignId: string = '';
  loading = true;
  error = '';
  data: any = {};
  stats: any = {};

  constructor(
    private trackingService: TrackingService,
    private route: ActivatedRoute,
    public authService: AuthService
  ) {}

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.campaignId = params['id'];
      this.loadCampaignDetail();
    });
  }

  loadCampaignDetail() {
    this.loading = true;
    this.trackingService.getCampaignDetails(this.campaignId).subscribe({
      next: (data) => {
        this.data = data;
        this.stats = data.stats || {};
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des détails';
        this.loading = false;
        console.error(err);
      }
    });
  }

  getStatusColor(status: string): string {
    const map: Record<string, string> = {
      'DRAFT': '#f59e0b',
      'AUTHORIZED': '#22c55e',
      'RUNNING': '#3b82f6',
      'PAUSED': '#6b7280',
      'SCHEDULED': '#8b5cf6',
      'COMPLETED': '#374151'
    };
    return map[status] || '#6b7280';
  }

  getFunnelPercent(value: number): number {
    const total = this.stats.total || 1;
    return (value / total) * 100;
  }

  getHourlyDistributionKeys(): string[] {
    const dist = this.stats.hourlyDistribution || {};
    return Object.keys(dist).sort((a, b) => Number(a) - Number(b));
  }

  getHourlyMax(): number {
    const dist = this.stats.hourlyDistribution || {};
    const values = Object.values(dist) as number[];
    return values.length > 0 ? Math.max(...values) : 1;
  }
}