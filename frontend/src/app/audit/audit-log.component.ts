import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../shared/navbar/navbar.component';
import { AuditService, AuditLog, AuditStats, IntegrityResult } from '../shared/services/audit.service';
import { AuthService } from '../shared/services/auth.service';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, NavbarComponent],
  templateUrl: './audit-log.component.html',
  styleUrls: ['./audit-log.component.scss']
})
export class AuditLogComponent implements OnInit {
  logs: AuditLog[] = [];
  filteredLogs: AuditLog[] = [];
  stats: AuditStats | null = null;
  integrity: IntegrityResult | null = null;
  
  loading = true;
  verifying = false;
  
  filterAction = new FormControl('');
  filterActor = new FormControl('');
  filterSearch = new FormControl('');

  actions = [
    { value: 'CAMPAIGN_CREATE', label: '📝 Création campagne' },
    { value: 'CAMPAIGN_UPDATE', label: '✏️ Mise à jour campagne' },
    { value: 'CAMPAIGN_AUTHORIZE', label: '✅ Autorisation campagne' },
    { value: 'CAMPAIGN_PAUSE', label: '⏸️ Pause campagne' },
    { value: 'CAMPAIGN_RESUME', label: '▶️ Reprise campagne' },
    { value: 'CAMPAIGN_DELETE', label: '🗑️ Suppression campagne' },
    { value: 'CAMPAIGN_CLONE', label: '📋 Clonage campagne' },
    { value: 'CAMPAIGN_AUTO_START', label: '⏰ Démarrage auto' },
    { value: 'CAMPAIGN_SEND', label: '📧 Envoi campagne' },
    { value: 'USER_CREATE', label: '👤 Création opérateur' },
    { value: 'USER_DELETE', label: '🚫 Suppression opérateur' }
  ];

  constructor(
    private auditService: AuditService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loadLogs();
    this.loadStats();
    this.verifyIntegrity();
  }

  loadLogs(): void {
    this.loading = true;
    this.auditService.getLogs().subscribe({
      next: (data) => {
        this.logs = data;
        this.filteredLogs = data;
        this.loading = false;
        this.applyFilters();
      },
      error: (err) => {
        console.error('Error loading audit logs:', err);
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.auditService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
      },
      error: (err) => {
        console.error('Error loading stats:', err);
      }
    });
  }

  verifyIntegrity(): void {
    this.verifying = true;
    this.auditService.verifyIntegrity().subscribe({
      next: (data) => {
        this.integrity = data;
        this.verifying = false;
      },
      error: (err) => {
        console.error('Error verifying integrity:', err);
        this.verifying = false;
        this.integrity = {
          valid: false,
          message: '❌ Erreur lors de la vérification',
          totalLogs: 0,
          checked: 0
        };
      }
    });
  }

  applyFilters(): void {
    const action = this.filterAction.value;
    const actor = this.filterActor.value;
    const search = this.filterSearch.value?.toLowerCase() || '';

    this.filteredLogs = this.logs.filter(log => {
      let match = true;

      if (action && log.action !== action) {
        match = false;
      }

      if (actor && !log.actor.toLowerCase().includes(actor.toLowerCase())) {
        match = false;
      }

      if (search) {
        const inDetails = log.details?.toLowerCase().includes(search) || false;
        const inAction = log.action.toLowerCase().includes(search);
        const inActor = log.actor.toLowerCase().includes(search);
        const inTarget = (log.target || '').toLowerCase().includes(search);
        if (!inDetails && !inAction && !inActor && !inTarget) {
          match = false;
        }
      }

      return match;
    });
  }

  clearFilters(): void {
    this.filterAction.setValue('');
    this.filterActor.setValue('');
    this.filterSearch.setValue('');
    this.applyFilters();
  }

  refresh(): void {
    this.loadAll();
  }

  getActionColor(action: string): string {
    const colors: Record<string, string> = {
      'CAMPAIGN_CREATE': '#22c55e',
      'CAMPAIGN_UPDATE': '#3b82f6',
      'CAMPAIGN_AUTHORIZE': '#8b5cf6',
      'CAMPAIGN_PAUSE': '#f59e0b',
      'CAMPAIGN_RESUME': '#22c55e',
      'CAMPAIGN_DELETE': '#dc2626',
      'CAMPAIGN_CLONE': '#8b5cf6',
      'CAMPAIGN_AUTO_START': '#3b82f6',
      'CAMPAIGN_SEND': '#e07b2a',
      'USER_CREATE': '#22c55e',
      'USER_DELETE': '#dc2626'
    };
    return colors[action] || '#6b7280';
  }

  getActionIcon(action: string): string {
    const icons: Record<string, string> = {
      'CAMPAIGN_CREATE': '➕',
      'CAMPAIGN_UPDATE': '✏️',
      'CAMPAIGN_AUTHORIZE': '✅',
      'CAMPAIGN_PAUSE': '⏸️',
      'CAMPAIGN_RESUME': '▶️',
      'CAMPAIGN_DELETE': '🗑️',
      'CAMPAIGN_CLONE': '📋',
      'CAMPAIGN_AUTO_START': '⏰',
      'CAMPAIGN_SEND': '📧',
      'USER_CREATE': '👤',
      'USER_DELETE': '🚫'
    };
    return icons[action] || '📌';
  }

  getActionLabel(action: string): string {
    const label = this.actions.find(a => a.value === action);
    return label ? label.label : action;
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }

  getHashShort(hash: string): string {
    return hash ? hash.substring(0, 12) + '...' : 'N/A';
  }

  getDetailsSummary(details: string): string {
    if (!details) return '—';
    try {
      const obj = JSON.parse(details);
      return obj.message || obj.campaignName || Object.values(obj).filter(v => typeof v === 'string').join(' ');
    } catch {
      return details.substring(0, 60) + (details.length > 60 ? '...' : '');
    }
  }

  getIntegrityColor(): string {
    if (this.integrity === null) return '#6b7280';
    return this.integrity.valid ? '#22c55e' : '#dc2626';
  }

  getIntegrityIcon(): string {
    if (this.integrity === null) return '⏳';
    return this.integrity.valid ? '✅' : '⚠️';
  }
}