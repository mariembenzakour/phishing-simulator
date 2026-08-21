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

  // ✅ AJOUTÉ : Map pour stocker le time-to-click par utilisateur
  userTimeToClickMap: Map<string, any> = new Map();

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
        
        // ✅ Charger le time-to-click pour chaque utilisateur
        this.loadUsersTimeToClick(data);
        
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des statistiques utilisateurs';
        this.loading = false;
        console.error(err);
      }
    });
  }

  // ✅ NOUVEAU : Charger le time-to-click pour tous les utilisateurs
  loadUsersTimeToClick(users: any[]) {
    users.forEach(user => {
      if (user.targetId) {
        this.trackingService.getUserTimeToClick(user.targetId).subscribe({
          next: (data) => {
            this.userTimeToClickMap.set(user.targetId, data);
          },
          error: () => {
            // Ignorer les erreurs silencieusement
          }
        });
      }
    });
  }

  // ✅ NOUVEAU : Récupérer le time-to-click formaté d'un utilisateur
  getUserTimeToClick(targetId: string): string {
    const data = this.userTimeToClickMap.get(targetId);
    return data?.formatted || 'N/A';
  }

  // ✅ NOUVEAU : Récupérer le temps moyen en secondes d'un utilisateur
  getUserTimeToClickSeconds(targetId: string): number {
    const data = this.userTimeToClickMap.get(targetId);
    return data?.averageSeconds || 0;
  }

  // ✅ NOUVEAU : Obtenir la couleur selon la rapidité du clic
  getTimeToClickColor(seconds: number): string {
    if (!seconds || seconds === 0) return '#6b7280';
    if (seconds < 60) return '#22c55e';      // < 1 min → vert
    if (seconds < 300) return '#8b5cf6';     // < 5 min → violet
    if (seconds < 900) return '#f59e0b';     // < 15 min → orange
    if (seconds < 3600) return '#dc2626';    // < 1h → rouge
    return '#6b7280';                         // > 1h → gris
  }

  // ✅ NOUVEAU : Formater une durée en secondes
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
      'OPEN': '',
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