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
  timeToClick: any = null;
  usersTimeToClick: any[] = [];

  constructor(
    private trackingService: TrackingService,
    private route: ActivatedRoute,
    public authService: AuthService
  ) {}

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.campaignId = params['id'];
      this.loadCampaignDetail();
      this.loadTimeToClick();
      this.loadUsersTimeToClick();
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

  // ✅ Time-to-click global
  loadTimeToClick() {
    this.trackingService.getTimeToClick(this.campaignId).subscribe({
      next: (data) => {
        this.timeToClick = data;
        console.log('⏱️ Time-to-click global:', data);
      },
      error: (err) => {
        console.error('Erreur time-to-click:', err);
      }
    });
  }

  // ✅ Time-to-click par utilisateur
  loadUsersTimeToClick() {
    this.trackingService.getUsersTimeToClick(this.campaignId).subscribe({
      next: (data) => {
        this.usersTimeToClick = data;
        console.log('👤 Time-to-click par utilisateur:', data);
      },
      error: (err) => {
        console.error('Erreur users time-to-click:', err);
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
    const dist = this.timeToClick?.hourlyDistribution || {};
    return Object.keys(dist).sort((a, b) => Number(a) - Number(b));
  }

  getHourlyMax(): number {
    const dist = this.timeToClick?.hourlyDistribution || {};
    const values = Object.values(dist) as number[];
    return values.length > 0 ? Math.max(...values) : 1;
  }

  // ✅ Formatage du time-to-click
  formatTimeToClick(seconds: number): string {
    if (!seconds || seconds === 0) return 'N/A';
    if (seconds < 60) {
      return Math.round(seconds) + 's';
    }
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.round(seconds % 60);
    if (minutes < 60) {
      return minutes + 'm ' + remainingSeconds + 's';
    }
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    if (hours < 24) {
      return hours + 'h ' + remainingMinutes + 'm';
    }
    const days = Math.floor(hours / 24);
    const remainingHours = hours % 24;
    return days + 'j ' + remainingHours + 'h';
  }

  // ✅ Couleur selon la rapidité du clic
  getTimeToClickColor(seconds: number): string {
    if (!seconds || seconds === 0) return '#6b7280';
    if (seconds < 60) return '#22c55e';      // < 1 min → vert
    if (seconds < 300) return '#8b5cf6';     // < 5 min → violet
    if (seconds < 900) return '#f59e0b';     // < 15 min → orange
    if (seconds < 3600) return '#dc2626';    // < 1h → rouge
    return '#6b7280';                         // > 1h → gris
  }

  // ✅ Obtenir les clés de distribution
  getDistributionKeys(): string[] {
    if (!this.timeToClick?.distribution) return [];
    return Object.keys(this.timeToClick.distribution);
  }

  // ✅ Obtenir la valeur de distribution
  getDistributionValue(key: string): number {
    return this.timeToClick?.distribution?.[key] || 0;
  }

  // ✅ Couleur pour la distribution
  getDistributionColor(key: string): string {
    const colors: Record<string, string> = {
      '< 1 min': '#22c55e',
      '1-5 min': '#8b5cf6',
      '5-15 min': '#f59e0b',
      '15-30 min': '#f97316',
      '30-60 min': '#dc2626',
      '> 1h': '#6b7280'
    };
    return colors[key] || '#6b7280';
  }
}