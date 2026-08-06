import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { TrackingService } from '../../shared/services/tracking.service';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-global-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  templateUrl: './global-dashboard.component.html'
})
export class GlobalDashboardComponent implements OnInit {

  loading = true;
  error = '';
  data: any = {};
  campaigns: any[] = [];

  constructor(
    private trackingService: TrackingService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    console.log('✅ GlobalDashboardComponent initialized');
    this.loadDashboard();
  }

  loadDashboard() {
    console.log('🔄 Loading dashboard...');
    this.loading = true;
    this.error = '';

    this.trackingService.getGlobalDashboard().subscribe({
      next: (data) => {
        console.log('✅ Dashboard data received:', data);
        this.data = data || {};
        this.campaigns = data?.campaigns || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Dashboard error:', err);
        this.error = 'Erreur lors du chargement du tableau de bord: ' + (err.message || err.statusText || '');
        this.loading = false;
      }
    });
  }

  goToCampaignDetail(campaignId: string) {
    this.router.navigate(['/dashboard/campaign', campaignId]);
  }

  goToUserReport() {
    this.router.navigate(['/dashboard/users']);
  }

  getStatusBadge(status: string): string {
    const map: Record<string, string> = {
      'DRAFT': 'badge-warning',
      'AUTHORIZED': 'badge-success',
      'RUNNING': 'badge-info',
      'PAUSED': 'badge-secondary',
      'SCHEDULED': 'badge-primary',
      'COMPLETED': 'badge-dark'
    };
    return map[status] || 'badge-secondary';
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
}