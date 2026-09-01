import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../shared/navbar/navbar.component';
import { AiService, AiGenerationLog, RedFlag } from '../shared/services/ai.service';
import { AuthService } from '../shared/services/auth.service';

@Component({
  selector: 'app-ai-drafts',
  standalone: true,
  imports: [CommonModule, NavbarComponent, RouterLink],
  templateUrl: './ai-drafts.component.html',
  styleUrls: ['./ai-drafts.component.scss']
})
export class AiDraftsComponent implements OnInit {

  drafts: AiGenerationLog[] = [];
  loading = true;
  error = '';
  success = '';
  total = 0;
  expandedDraftId: string | null = null;

  constructor(
    private aiService: AiService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadDrafts();
  }

  loadDrafts() {
    this.loading = true;
    this.aiService.getApprovedDrafts().subscribe({
      next: (data) => {
        this.drafts = data.drafts || [];
        this.total = data.total || 0;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des drafts';
        this.loading = false;
      }
    });
  }

  approve(id: string) {
    this.aiService.approve(id).subscribe({
      next: () => {
        this.success = 'Draft approuvé !';
        this.loadDrafts();
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors de l\'approbation';
      }
    });
  }

  reject(id: string) {
    if (!confirm('Rejeter ce draft ?')) return;
    this.aiService.reject(id).subscribe({
      next: () => {
        this.success = 'Draft rejeté !';
        this.loadDrafts();
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du rejet';
      }
    });
  }

  toggleExpand(id: string) {
    this.expandedDraftId = this.expandedDraftId === id ? null : id;
  }

  getStatusBadge(approved: boolean): string {
    return approved ? 'Approuvé' : 'En attente';
  }

  getStatusColor(approved: boolean): string {
    return approved ? '#059669' : '#d97706';
  }

  isAdmin(): boolean {
    return this.authService.isAdmin() || this.authService.isSuperAdmin();
  }

  getLanguageLabel(lang: string): string {
    const map: Record<string, string> = {
      'fr': 'Français',
      'en': 'English'
    };
    return map[lang] || lang;
  }

  parseRedFlags(redFlagsJson: string): RedFlag[] {
    try {
      return JSON.parse(redFlagsJson);
    } catch {
      return [];
    }
  }

  getSeverityIcon(severity: string): string {
    const map: Record<string, string> = {
      'critical': '#dc2626',
      'high': '#ea580c',
      'medium': '#d97706',
      'low': '#059669'
    };
    return map[severity] || '#64748b';
  }
}