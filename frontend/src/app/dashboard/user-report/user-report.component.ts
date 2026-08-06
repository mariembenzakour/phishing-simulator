import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { TrackingService } from '../../shared/services/tracking.service';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-user-report',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  templateUrl: './user-report.component.html'
})
export class UserReportComponent implements OnInit {

  loading = true;
  error = '';
  users: any[] = [];
  repeatClickers: any[] = [];
  selectedUser: any = null;
  userHistory: any[] = [];
  showHistory = false;

  constructor(
    private trackingService: TrackingService,
    public authService: AuthService
  ) {}

  ngOnInit() {
    this.loadUserStats();
  }

  loadUserStats() {
    this.loading = true;
    this.trackingService.getUserStats().subscribe({
      next: (data) => {
        this.users = data;
        this.repeatClickers = data.filter((u: any) => u.totalClicks > 1);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des statistiques utilisateurs';
        this.loading = false;
        console.error(err);
      }
    });
  }

  viewUserHistory(user: any) {
    this.selectedUser = user;
    this.showHistory = true;
    this.loading = true;
    this.trackingService.getUserHistory(user.targetId).subscribe({
      next: (data) => {
        this.userHistory = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement de l\'historique';
        this.loading = false;
        console.error(err);
      }
    });
  }

  closeHistory() {
    this.showHistory = false;
    this.selectedUser = null;
    this.userHistory = [];
  }

  getTotalInteractions(user: any): number {
    return (user.totalOpens || 0) + (user.totalClicks || 0) + (user.totalSubmits || 0);
  }

  isRepeatClicker(user: any): boolean {
    return (user.totalClicks || 0) > 1;
  }

  getEventIcon(type: string): string {
    const map: Record<string, string> = {
      'OPEN': '👁️',
      'CLICK': '🖱️',
      'SUBMIT': '📝'
    };
    return map[type] || '📌';
  }

  getEventColor(type: string): string {
    const map: Record<string, string> = {
      'OPEN': '#60a5fa',
      'CLICK': '#a78bfa',
      'SUBMIT': '#fbbf24'
    };
    return map[type] || '#8899aa';
  }
}